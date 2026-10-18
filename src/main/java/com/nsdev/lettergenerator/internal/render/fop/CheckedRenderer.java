package com.nsdev.lettergenerator.internal.render.fop;

import com.nsdev.lettergenerator.internal.layout.Units;
import com.nsdev.lettergenerator.internal.render.fo.PageMaster;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.inline.AbstractTextArea;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.WordArea;
import org.apache.fop.fo.Constants;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.render.intermediate.IFRenderer;

/**
 * FOP intermediate renderer that verifies painted glyph and underline bounds, not just line boxes.
 *
 * <p>FOP may accept a logical line whose italic side bearing, ascender or underline paints outside
 * a fixed rectangle. This renderer tracks the active region/box and scaled font metrics in
 * millipoints, records those failures, and reports the actual page masters for final-page
 * verification. All state is confined to one engine attempt.
 */
final class CheckedRenderer extends IFRenderer {
    /** Allowed sub-point numeric rounding at every checked ink boundary, in millipoints. */
    private static final double ROUNDING_TOLERANCE_MPT = 1.0;

    // Matches FOP 2.11 AbstractPathOrientedRenderer's underline stroke calculation.
    /** Divisor reproducing FOP's descender-based underline stroke width. */
    private static final double UNDERLINE_DESCENDER_DIVISOR = 8.0;

    /** Index of horizontal glyph placement in FOP's four-entry adjustment array. */
    private static final int X_PLACEMENT_INDEX = 0;

    /** Index of vertical glyph placement in FOP's four-entry adjustment array. */
    private static final int Y_PLACEMENT_INDEX = 1;

    /** Index of horizontal cursor advance in FOP's four-entry adjustment array. */
    private static final int X_ADVANCE_INDEX = 2;

    /** Index of vertical cursor advance in FOP's four-entry adjustment array. */
    private static final int Y_ADVANCE_INDEX = 3;

    /**
     * The current clipping rectangle in millipoints; body regions allow horizontal side bearings
     * into page margins, fixed boxes do not.
     *
     * @param widthMpt horizontal extent of the active layout box in millipoints
     * @param heightMpt vertical extent of the active layout box in millipoints
     * @param horizontalClip whether side bearings must remain within the box width
     * @param bodyRegion whether bottom-only overflow can be classified as body recovery
     */
    private record LayoutBox(
            double widthMpt, double heightMpt, boolean horizontalClip, boolean bodyRegion) {}

    /**
     * Per-glyph placement/advance corrections from FOP, needed to compare actual painted positions
     * with box boundaries.
     *
     * @param offsetX horizontal placement correction in millipoints
     * @param offsetY vertical placement correction in millipoints
     * @param advanceX additional horizontal cursor advance in millipoints
     * @param advanceY additional vertical cursor advance in millipoints
     */
    private record GlyphAdjustment(
            double offsetX, double offsetY, double advanceX, double advanceY) {
        /** Shared zero adjustment for words lacking glyph or legacy letter corrections. */
        private static final GlyphAdjustment NONE = new GlyphAdjustment(0, 0, 0, 0);

        /**
         * Reads positioning-array adjustments when present, falls back to legacy letter
         * adjustments, and otherwise returns a zero displacement.
         *
         * @param word word supplying positional or legacy letter adjustment arrays
         * @param index UTF-16 word-text index used to look up the current code point's adjustment
         * @return positional correction, a legacy horizontal correction, or the zero adjustment
         */
        static GlyphAdjustment at(WordArea word, int index) {
            int[][] positions = word.getGlyphPositionAdjustments();
            if (positions != null && index < positions.length && positions[index] != null) {
                int[] adjustment = positions[index];
                return new GlyphAdjustment(
                        adjustment[X_PLACEMENT_INDEX],
                        adjustment[Y_PLACEMENT_INDEX],
                        adjustment[X_ADVANCE_INDEX],
                        adjustment[Y_ADVANCE_INDEX]);
            }
            int[] letters = word.getLetterAdjustArray();
            if (letters != null && index < letters.length) {
                return new GlyphAdjustment(letters[index], 0, letters[index], 0);
            }
            return NONE;
        }
    }

    /** Enclosing region or absolute fixed box, temporarily replaced during nested rendering. */
    private LayoutBox activeBox;

    /** Current text baseline in top-down millipoints, restored after each text area. */
    private int baselineMpt;

    /** Distinct painted-bound failures in encounter order for this rendering attempt. */
    private final Set<RenderProblem> inkProblems = new LinkedHashSet<>();

    /** Actual selected masters keyed by zero-based page index for stable page-order reporting. */
    private final SortedMap<Integer, PageMaster> masters = new TreeMap<>();

    /**
     * Creates a checked intermediate renderer for the current user agent; no instance is shared
     * across rendering operations.
     *
     * @param agent user agent owned by the current rendering attempt
     */
    CheckedRenderer(FOUserAgent agent) {
        super(agent);
    }

    /**
     * Returns distinct painted-bound problems gathered so far as an immutable snapshot.
     *
     * @return immutable distinct ink-bound diagnostics in encounter order
     */
    List<RenderProblem> problems() {
        return List.copyOf(inkProblems);
    }

    /**
     * Returns the actual master used by each rendered page, sorted by page index.
     *
     * @return immutable selected masters in ascending zero-based page-index order
     */
    List<PageMaster> masters() {
        return List.copyOf(masters.values());
    }

    /**
     * Records the actual page master before delegating PDF painting to the intermediate renderer.
     *
     * @param page laid-out page whose master is recorded before painting
     * @throws IOException if the underlying renderer cannot write page output
     * @throws FOPException if FOP cannot render the page
     */
    @Override
    public void renderPage(PageViewport page) throws IOException, FOPException {
        masters.put(page.getPageIndex(), PageMaster.fromIdentifier(page.getSimplePageMasterName()));
        super.renderPage(page);
    }

    /**
     * Pushes the region's dimensions and body identity for nested ink checks, restoring the
     * previous clipping context afterward.
     *
     * @param viewport region dimensions and identity used while rendering nested areas
     */
    @Override
    protected void renderRegionViewport(RegionViewport viewport) {
        LayoutBox previous = activeBox;
        // Body side bearings may use the page margin. Vertical ink must respect reserved zones.
        activeBox =
                new LayoutBox(
                        viewport.getIPD(),
                        viewport.getBPD(),
                        false,
                        viewport.getRegionReference().getRegionClass() == Constants.FO_REGION_BODY);
        try {
            super.renderRegionViewport(viewport);
        } finally {
            activeBox = previous;
        }
    }

    /**
     * Pushes stricter clipping dimensions for absolutely positioned fixed boxes; normal flow keeps
     * its enclosing region constraints.
     *
     * @param viewport block whose absolute positioning can introduce a tighter clipping box
     * @param children child areas passed unchanged to the intermediate renderer
     */
    @Override
    @SuppressWarnings("rawtypes") // FOP 2.11 declares this override with a raw List.
    protected void renderBlockViewport(BlockViewport viewport, List children) {
        LayoutBox previous = activeBox;
        if (viewport.getPositioning() == org.apache.fop.area.Block.ABSOLUTE) {
            activeBox = new LayoutBox(viewport.getIPD(), viewport.getBPD(), true, false);
        }
        try {
            super.renderBlockViewport(viewport, children);
        } finally {
            activeBox = previous;
        }
    }

    /**
     * Tracks the resolved text baseline before FOP dispatches glyph callbacks, restoring it after
     * the text area is rendered.
     *
     * @param text text area providing the current baseline and child glyph areas
     */
    @Override
    protected void renderText(TextArea text) {
        int previous = baselineMpt;
        baselineMpt =
                currentBPPosition + text.getBlockProgressionOffset() + text.getBaselineOffset();
        try {
            super.renderText(text);
        } finally {
            baselineMpt = previous;
        }
    }

    /**
     * Checks word ink when a layout box is active, then lets the engine paint the original word
     * unchanged.
     *
     * @param word word checked against the active box before normal painting
     */
    @Override
    protected void renderWord(WordArea word) {
        if (activeBox != null) {
            checkWord(word);
        }
        super.renderWord(word);
    }

    /**
     * Walks Unicode code points using actual font widths, ink bounds and glyph adjustments. Font
     * y-up metrics are inverted into FOP's top-down box coordinates.
     *
     * @param word word whose resolved font and placement corrections determine painted ink bounds
     */
    private void checkWord(WordArea word) {
        org.apache.fop.fonts.Font font = getFontFromArea(word.getParentArea());
        FontMetrics metrics = font.getFontMetrics();
        AbstractTextArea parent = (AbstractTextArea) word.getParentArea();
        double cursorX = currentIPPosition;
        double cursorY = baselineMpt;
        String text = word.getWord();
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            GlyphAdjustment adjustment = GlyphAdjustment.at(word, index);
            Rectangle ink = metrics.getBoundingBox(font.mapCodePoint(codePoint), 1);
            if (ink != null && ink.width > 0 && ink.height > 0) {
                int size = font.getFontSize();
                checkInkBounds(
                        cursorX
                                + adjustment.offsetX()
                                + Units.fontUnitsToMillipoints(ink.getMinX(), size),
                        cursorY
                                + adjustment.offsetY()
                                - Units.fontUnitsToMillipoints(ink.getMaxY(), size),
                        cursorX
                                + adjustment.offsetX()
                                + Units.fontUnitsToMillipoints(ink.getMaxX(), size),
                        cursorY
                                + adjustment.offsetY()
                                - Units.fontUnitsToMillipoints(ink.getMinY(), size));
            }
            cursorX +=
                    font.getCharWidth(codePoint)
                            + parent.getTextLetterSpaceAdjust()
                            + adjustment.advanceX();
            cursorY += adjustment.advanceY();
            index += Character.charCount(codePoint);
        }
    }

    /**
     * Checks underline ink using FOP's descender-based stroke calculation before delegating
     * decoration painting.
     *
     * @param metrics resolved physical face metrics used by FOP for decoration placement
     * @param size resolved font size in millipoints
     * @param inline inline area supplying underline state and horizontal extent
     * @param baseline resolved text baseline in millipoints
     * @param start underline start position in millipoints
     */
    @Override
    protected void renderTextDecoration(
            FontMetrics metrics, int size, InlineArea inline, int baseline, int start) {
        if (activeBox != null && inline.hasUnderline()) {
            double descender = Units.fontUnitsToMillipoints(metrics.getDescender(1), size);
            double halfWidth = (-descender / UNDERLINE_DESCENDER_DIVISOR) / 2;
            double verticalPosition = baseline - descender / 2;
            checkInkBounds(
                    start,
                    verticalPosition - halfWidth,
                    start + inline.getIPD(),
                    verticalPosition + halfWidth);
        }
        super.renderTextDecoration(metrics, size, inline, baseline, start);
    }

    /**
     * Records out-of-box ink with a one-millipoint rounding allowance. Only bottom overflow in body
     * space, without horizontal overflow, is eligible for signature-page recovery.
     *
     * @param left leftmost painted coordinate in millipoints within the active box
     * @param top topmost painted coordinate in top-down millipoints within the active box
     * @param right rightmost painted coordinate in millipoints within the active box
     * @param bottom bottommost painted coordinate in top-down millipoints within the active box
     */
    private void checkInkBounds(double left, double top, double right, double bottom) {
        boolean verticalOverflow =
                top < -ROUNDING_TOLERANCE_MPT
                        || bottom > activeBox.heightMpt() + ROUNDING_TOLERANCE_MPT;
        boolean horizontalOverflow =
                activeBox.horizontalClip()
                        && (left < -ROUNDING_TOLERANCE_MPT
                                || right > activeBox.widthMpt() + ROUNDING_TOLERANCE_MPT);
        if (verticalOverflow || horizontalOverflow) {
            String message =
                    "Painted glyph or decoration extends outside its layout box: ["
                            + left
                            + ","
                            + top
                            + ","
                            + right
                            + ","
                            + bottom
                            + "] within "
                            + activeBox;
            boolean recoverableBodyOverflow =
                    activeBox.bodyRegion()
                            && top >= -ROUNDING_TOLERANCE_MPT
                            && bottom > activeBox.heightMpt() + ROUNDING_TOLERANCE_MPT
                            && !horizontalOverflow;
            inkProblems.add(new RenderProblem(message, recoverableBodyOverflow));
        }
    }
}
