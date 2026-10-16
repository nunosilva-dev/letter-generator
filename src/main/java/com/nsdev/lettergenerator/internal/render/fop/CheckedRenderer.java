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

/** Checks painted glyph bounds in FOP's millipoint coordinates before bytes can escape. */
final class CheckedRenderer extends IFRenderer {
    private static final double ROUNDING_TOLERANCE_MPT = 1.0;
    // Matches FOP 2.11 AbstractPathOrientedRenderer's underline stroke calculation.
    private static final double UNDERLINE_DESCENDER_DIVISOR = 8.0;
    private static final int X_PLACEMENT_INDEX = 0;
    private static final int Y_PLACEMENT_INDEX = 1;
    private static final int X_ADVANCE_INDEX = 2;
    private static final int Y_ADVANCE_INDEX = 3;

    private record LayoutBox(
            double widthMpt, double heightMpt, boolean horizontalClip, boolean bodyRegion) {}

    private record GlyphAdjustment(
            double offsetX, double offsetY, double advanceX, double advanceY) {
        private static final GlyphAdjustment NONE = new GlyphAdjustment(0, 0, 0, 0);

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

    private LayoutBox activeBox;
    private int baselineMpt;
    private final Set<RenderProblem> inkProblems = new LinkedHashSet<>();
    private final SortedMap<Integer, PageMaster> masters = new TreeMap<>();

    CheckedRenderer(FOUserAgent agent) {
        super(agent);
    }

    List<RenderProblem> problems() {
        return List.copyOf(inkProblems);
    }

    List<PageMaster> masters() {
        return List.copyOf(masters.values());
    }

    @Override
    public void renderPage(PageViewport page) throws IOException, FOPException {
        masters.put(page.getPageIndex(), PageMaster.fromIdentifier(page.getSimplePageMasterName()));
        super.renderPage(page);
    }

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

    @Override
    protected void renderWord(WordArea word) {
        if (activeBox != null) {
            checkWord(word);
        }
        super.renderWord(word);
    }

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
