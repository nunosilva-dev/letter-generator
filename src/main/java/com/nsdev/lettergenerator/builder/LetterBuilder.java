package com.nsdev.lettergenerator.builder;

import com.nsdev.lettergenerator.component.LetterComponent;
import com.nsdev.lettergenerator.document.PdfAttachment;
import com.nsdev.lettergenerator.internal.builder.HeaderLayout;
import com.nsdev.lettergenerator.internal.builder.LetterParts;
import com.nsdev.lettergenerator.internal.builder.LetterRegions;
import com.nsdev.lettergenerator.internal.builder.RegionVariants;
import com.nsdev.lettergenerator.internal.builder.content.ContentPart;
import com.nsdev.lettergenerator.internal.builder.content.ContentParts;
import com.nsdev.lettergenerator.internal.content.ImageBlocks;
import com.nsdev.lettergenerator.internal.content.PlainText;
import com.nsdev.lettergenerator.internal.defaults.LetterLayoutDefaults;
import com.nsdev.lettergenerator.internal.util.ArgumentChecks;
import com.nsdev.lettergenerator.letter.AddressLayout;
import com.nsdev.lettergenerator.letter.LetterElement;
import com.nsdev.lettergenerator.letter.Party;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.DocumentMetadata;
import com.nsdev.lettergenerator.model.DocumentSettings;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.CanvasBlock;
import com.nsdev.lettergenerator.model.block.ComponentBlock;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.Orientation;
import com.nsdev.lettergenerator.model.layout.PageDimensions;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.TextDirection;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import com.nsdev.lettergenerator.model.text.TextRun;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import com.nsdev.lettergenerator.signature.SignatureContent;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable builder for ordinary letters, obtained from {@link
 * com.nsdev.lettergenerator.Letter#builder()} or the public no-argument constructor. Body content,
 * tables, blocks and fixed elements append in call order; header, footer, signature, logo,
 * watermark and configuration calls replace the corresponding setting. Each {@link #build()}
 * returns an independent immutable {@link LetterDocument}; the builder remains reusable and is not
 * intended for concurrent use.
 *
 * <p>Defaults are A4 with 20 mm margins, 3 mm paragraph spacing, a 25 mm header box, a 15 mm footer
 * box, and a flowing signature. HTML signature images use a 20 mm height basis by default. Body
 * typography defaults to 11 pt black Helvetica with a 1.2 line-height multiplier. Header and footer
 * reserve space only when present, with a 5 mm gap to the body; footer text defaults to 9 pt and
 * {@code #666666}. Images preserve their aspect ratio. Named assets and HTML regions resolve
 * against final builder settings at build time.
 *
 * <p>Argument checks catch nulls and basic geometry during configuration or building. The resulting
 * model still needs {@link com.nsdev.lettergenerator.LetterPdfGenerator} validation for fonts,
 * styles, collisions and final layout.
 */
public final class LetterBuilder {
    /** Default body paragraph after-spacing in millimeters. */
    private static final double DEFAULT_PARAGRAPH_GAP_MM = 3.0;

    /** Default footer font size in typographic points. */
    private static final double DEFAULT_FOOTER_SIZE_PT = 9.0;

    /** Default footer RGB text color. */
    private static final String DEFAULT_FOOTER_COLOR = "#666666";

    /** Stable name assigned to the fluent builder's single body section. */
    private static final String BODY_SECTION_NAME = "body";

    /** Body paragraph spacing overlaid below application-supplied style overrides. */
    private static final Style DEFAULT_BODY_STYLE =
            Style.builder().spaceAfterMm(DEFAULT_PARAGRAPH_GAP_MM).build();

    /** Header defaults suppress paragraph spacing inside the fixed header box. */
    private static final Style DEFAULT_HEADER_STYLE =
            Style.builder().spaceBeforeMm(0).spaceAfterMm(0).build();

    /** Footer text defaults layered over the zero-spacing header defaults. */
    private static final Style DEFAULT_FOOTER_STYLE =
            DEFAULT_HEADER_STYLE.overlay(
                    Style.builder()
                            .fontSizePt(DEFAULT_FOOTER_SIZE_PT)
                            .color(DEFAULT_FOOTER_COLOR)
                            .build());

    /** Initial maximum image width in millimeters before final header/footer fitting. */
    private static final double DEFAULT_REGION_IMAGE_WIDTH_MM = 160.0;

    /** Ordered appended body parts, including deferred HTML fragments. */
    private final List<ContentPart> body = new ArrayList<>();

    /** Deferred semantic fields assembled around body flow using final settings. */
    private final LetterParts letterParts = new LetterParts();

    /** Owned image byte copies available to all deferred markup fragments. */
    private final Map<String, byte[]> assets = new LinkedHashMap<>();

    /** Additional advanced fixed elements appended after generated region elements. */
    private final List<FixedElement> fixedElements = new ArrayList<>();

    /** Current paper dimensions; region geometry is calculated only at build time. */
    private PageSize page = PageSize.A4;

    /** Optional custom physical sheet dimensions. */
    private PageDimensions dimensions;

    /** Explicit sheet orientation. */
    private Orientation orientation = Orientation.PORTRAIT;

    /** Document-local language, typography and metadata options. */
    private DocumentSettings settings = DocumentSettings.defaults();

    /** Ordered PDFs appended after the rendered letter. */
    private final List<PdfAttachment> attachments = new ArrayList<>();

    /** Current page-edge margins in millimeters. */
    private Margins margins = Margins.defaults();

    /** Current body defaults, including the baseline paragraph gap. */
    private Style style = Style.plain();

    /** Optional per-document theme. */
    private LetterTheme theme;

    /** Reusable immutable content registrations. */
    private final Map<String, LetterComponent> components = new LinkedHashMap<>();

    /** Current header typography and alignment defaults applied during content resolution. */
    private Style headerStyle = Style.plain();

    /** Current footer typography defaults applied during content resolution. */
    private Style footerStyle = Style.plain();

    /** Latest header content; an empty part produces no repeating header element. */
    private final RegionVariants header = new RegionVariants();

    /** Latest footer content; an empty part produces no repeating footer element. */
    private final RegionVariants footer = new RegionVariants();

    /** Current logo snapshot, or null when no logo has been supplied. */
    private ImageBlock logo;

    /** Deferred automatic or exact header placement using final page/logo settings. */
    private HeaderLayout headerLayout = HeaderLayout.defaults();

    /** Current footer content height in millimeters, excluding the body gap. */
    private double footerHeight = LetterLayoutDefaults.FOOTER_HEIGHT_MM;

    /** Latest signature content, or null when no signature has been supplied. */
    private ContentPart signature;

    /** Current flow or final-page anchoring policy for signature content. */
    private SignaturePlacement signaturePlacement = SignaturePlacement.FLOW;

    /** HTML image height basis and optional reserved signature-zone height in millimeters. */
    private double signatureHeight = LetterLayoutDefaults.SIGNATURE_HEIGHT_MM;

    /** Latest PDF post-processing watermark, or null when absent. */
    private Watermark watermark;

    /**
     * Replaces one semantic role while preserving conventional letter order.
     *
     * @param element semantic role
     * @param blocks immutable-model content
     * @return this builder
     */
    public LetterBuilder part(LetterElement element, List<Block> blocks) {
        return part(element, blocks, null);
    }

    /**
     * Replaces one semantic role with explicit local wrapper styling.
     *
     * @param element semantic role
     * @param blocks ordered model content
     * @param localStyle optional local style
     * @return this builder
     */
    public LetterBuilder part(LetterElement element, List<Block> blocks, Style localStyle) {
        letterParts.part(element, blocks, localStyle);
        return this;
    }

    /**
     * Replaces the sender identity/address.
     *
     * @param value non-null validated sender
     * @return this builder
     */
    public LetterBuilder sender(Party value) {
        letterParts.party(LetterElement.SENDER, value);
        return this;
    }

    /**
     * Replaces the recipient identity/address.
     *
     * @param value non-null validated recipient
     * @return this builder
     */
    public LetterBuilder recipient(Party value) {
        letterParts.party(LetterElement.RECIPIENT, value);
        return this;
    }

    /**
     * Defers date formatting until final builder locale is known.
     *
     * @param value non-null letter date
     * @return this builder
     */
    public LetterBuilder date(LocalDate value) {
        return date(value, null);
    }

    /**
     * Defers a locally styled date until final locale is known.
     *
     * @param value non-null date
     * @param localStyle optional local style
     * @return this builder
     */
    public LetterBuilder date(LocalDate value, Style localStyle) {
        letterParts.date(value, localStyle);
        return this;
    }

    /**
     * Replaces the nonblank literal subject.
     *
     * @param value subject text
     * @return this builder
     */
    public LetterBuilder subject(String value) {
        letterParts.text(LetterElement.SUBJECT, value);
        return this;
    }

    /**
     * Replaces the reference with a bold label and literal value.
     *
     * @param label nonblank label
     * @param value nonblank value
     * @return this builder
     */
    public LetterBuilder reference(String label, String value) {
        letterParts.reference(label, value);
        return this;
    }

    /**
     * Replaces the nonblank opening greeting.
     *
     * @param value greeting
     * @return this builder
     */
    public LetterBuilder salutation(String value) {
        letterParts.text(LetterElement.SALUTATION, value);
        return this;
    }

    /**
     * Replaces the nonblank closing phrase after body content.
     *
     * @param value closing phrase
     * @return this builder
     */
    public LetterBuilder closing(String value) {
        letterParts.text(LetterElement.CLOSING, value);
        return this;
    }

    /**
     * Places configured sender/recipient/date fields inside an editable body-relative panel.
     *
     * @param value optional-bound layout preset
     * @return this builder
     */
    public LetterBuilder addressLayout(AddressLayout value) {
        letterParts.addressLayout(value);
        return this;
    }

    /**
     * Appends one bounded arbitrary-position body canvas.
     *
     * @param value immutable canvas
     * @return this builder
     */
    public LetterBuilder canvas(CanvasBlock value) {
        return block(ArgumentChecks.required(value, "canvas"));
    }

    /**
     * Replaces optional language, font and PDF metadata settings.
     *
     * @param value settings, null selects defaults
     * @return this builder
     */
    public LetterBuilder settings(DocumentSettings value) {
        settings = value == null ? DocumentSettings.defaults() : value;
        return this;
    }

    /**
     * Sets deterministic country-aware formatting.
     *
     * @param value language and formatting settings
     * @return this builder
     */
    public LetterBuilder locale(LocaleSettings value) {
        settings = settings.withLocale(ArgumentChecks.required(value, "locale"));
        return this;
    }

    /**
     * Sets a locale using its language tag and default date/currency conventions.
     *
     * @param value country-aware locale
     * @return this builder
     */
    public LetterBuilder locale(java.util.Locale value) {
        return locale(new LocaleSettings(ArgumentChecks.required(value, "locale").toLanguageTag()));
    }

    /**
     * Replaces PDF metadata.
     *
     * @param value optional title, author, subject and keywords
     * @return this builder
     */
    public LetterBuilder metadata(DocumentMetadata value) {
        settings =
                new DocumentSettings(
                        settings.locale(),
                        value,
                        settings.direction(),
                        settings.fallbackFonts(),
                        settings.hyphenate());
        return this;
    }

    /**
     * Sets writing direction independently from formatting locale.
     *
     * @param value explicit direction or AUTO for language-derived direction
     * @return this builder
     */
    public LetterBuilder direction(TextDirection value) {
        settings =
                new DocumentSettings(
                        settings.locale(),
                        settings.metadata(),
                        value,
                        settings.fallbackFonts(),
                        settings.hyphenate());
        return this;
    }

    /**
     * Sets ordered explicit registered fallback font families.
     *
     * @param values registered family names, tried after the selected family
     * @return this builder
     */
    public LetterBuilder fallbackFonts(String... values) {
        settings =
                new DocumentSettings(
                        settings.locale(),
                        settings.metadata(),
                        settings.direction(),
                        Arrays.asList(values),
                        settings.hyphenate());
        return this;
    }

    /**
     * Enables dictionary hyphenation using explicitly registered trusted patterns.
     *
     * @param value whether hyphenation should be enabled
     * @return this builder
     */
    public LetterBuilder hyphenate(boolean value) {
        settings =
                new DocumentSettings(
                        settings.locale(),
                        settings.metadata(),
                        settings.direction(),
                        settings.fallbackFonts(),
                        value);
        return this;
    }

    /**
     * Appends an immutable existing PDF to the output pack.
     *
     * @param value validated attachment snapshot
     * @return this builder
     */
    public LetterBuilder attach(PdfAttachment value) {
        attachments.add(ArgumentChecks.required(value, "attachment"));
        return this;
    }

    /**
     * Selects custom physical paper dimensions, preserving the supplied orientation.
     *
     * @param widthMm sheet width in millimeters
     * @param heightMm sheet height in millimeters
     * @return this builder
     */
    public LetterBuilder pageSizeMm(double widthMm, double heightMm) {
        dimensions =
                new PageDimensions(
                        ArgumentChecks.positive(widthMm, "page width"),
                        ArgumentChecks.positive(heightMm, "page height"));
        orientation = widthMm > heightMm ? Orientation.LANDSCAPE : Orientation.PORTRAIT;
        return this;
    }

    /**
     * Selects the shorter or longer edge as the horizontal edge.
     *
     * @param value sheet orientation
     * @return this builder
     */
    public LetterBuilder orientation(Orientation value) {
        orientation = ArgumentChecks.required(value, "orientation");
        return this;
    }

    /**
     * @return this builder configured for landscape paper
     */
    public LetterBuilder landscape() {
        return orientation(Orientation.LANDSCAPE);
    }

    /**
     * Appends an immutable fragment as a softly kept body group.
     *
     * @param value reusable content
     * @return this builder
     */
    public LetterBuilder body(LetterComponent value) {
        return block(new GroupBlock(ArgumentChecks.required(value, "component").blocks(), null));
    }

    /**
     * Replaces header content with a reusable fragment.
     *
     * @param value reusable content
     * @return this builder
     */
    public LetterBuilder header(LetterComponent value) {
        return headerBlocks(
                ArgumentChecks.required(value, "component").blocks().toArray(Block[]::new));
    }

    /**
     * Replaces footer content with a reusable fragment.
     *
     * @param value reusable content
     * @return this builder
     */
    public LetterBuilder footer(LetterComponent value) {
        return footerBlocks(
                ArgumentChecks.required(value, "component").blocks().toArray(Block[]::new));
    }

    /**
     * Replaces signature content with a reusable fragment.
     *
     * @param value reusable content
     * @return this builder
     */
    public LetterBuilder signature(LetterComponent value) {
        return signatureBlocks(
                ArgumentChecks.required(value, "component").blocks().toArray(Block[]::new));
    }

    /**
     * Sets role defaults and named styles for this document.
     *
     * @param value non-null theme
     * @return this builder
     */
    public LetterBuilder theme(LetterTheme value) {
        theme = ArgumentChecks.required(value, "theme");
        return this;
    }

    /**
     * Registers a reusable document-local fragment; later registrations replace the same name.
     *
     * @param name nonblank reference name
     * @param value immutable content
     * @return this builder
     */
    public LetterBuilder component(String name, LetterComponent value) {
        ArgumentChecks.require(name != null && !name.isBlank(), "Component name must not be blank");
        components.put(name, ArgumentChecks.required(value, "component"));
        return this;
    }

    /**
     * Appends a reference to a registered fragment at this position in body flow.
     *
     * @param name component name resolved during generation
     * @return this builder
     */
    public LetterBuilder include(String name) {
        return block(new ComponentBlock(name, null));
    }

    /**
     * Replaces the paper size, initially {@link PageSize#A4}. Header geometry and image fitting use
     * the selected size when built.
     *
     * @param size non-null paper size
     * @return this builder
     * @throws IllegalArgumentException if {@code size} is null
     */
    public LetterBuilder page(PageSize size) {
        page = ArgumentChecks.required(size, "page size");
        dimensions = null;
        return this;
    }

    /**
     * Replaces all four page margins with the same millimeter value. Margins must leave positive
     * body space when the document is built.
     *
     * @param allSides finite nonnegative distance from every paper edge, in mm
     * @return this builder
     * @throws IllegalArgumentException if the value is negative or nonfinite
     */
    public LetterBuilder marginsMm(double allSides) {
        return margins(Margins.uniform(ArgumentChecks.nonnegative(allSides, "margins")));
    }

    /**
     * Replaces the four page margins. Nonnegative values and available page space are checked when
     * building, allowing the page size to be changed before then.
     *
     * @param value non-null top, right, bottom and left margins in mm
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public LetterBuilder margins(Margins value) {
        margins = ArgumentChecks.required(value, "margins");
        return this;
    }

    /**
     * Replaces body style overrides, overlaid on the default 3 mm paragraph gap. Specified block or
     * run styles can override inherited fields. This call does not accumulate overrides from
     * earlier calls to this method.
     *
     * @param value non-null body defaults; unspecified fields retain library defaults
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public LetterBuilder style(Style value) {
        style = ArgumentChecks.required(value, "style");
        return this;
    }

    /**
     * Appends plain text chunks in order. Single newlines change lines, blank lines separate
     * paragraphs, and form feeds (including {@link com.nsdev.lettergenerator.Letter#PAGE_BREAK})
     * insert page breaks. Each chunk begins its own paragraph sequence; CRLF and CR are normalized.
     *
     * @param content non-null array of non-null body strings
     * @return this builder
     * @throws IllegalArgumentException if the array or a chunk is null
     */
    public LetterBuilder body(String... content) {
        body.add(ContentParts.blocks(PlainText.body(content)));
        return this;
    }

    /**
     * Appends a heading block whose appearance inherits the document style and heading defaults.
     *
     * @param text non-null heading text
     * @return this builder
     * @throws IllegalArgumentException if {@code text} is null
     */
    public LetterBuilder heading(String text) {
        return block(
                new Heading(
                        List.of(new TextRun(ArgumentChecks.required(text, "heading"), null, null)),
                        null));
    }

    /**
     * Appends an explicit break so subsequent body content begins on a new page.
     *
     * @return this builder
     */
    public LetterBuilder pageBreak() {
        return block(new PageBreakBlock());
    }

    /**
     * Appends a table block, commonly produced by {@link
     * com.nsdev.lettergenerator.Table#builder(String...)}.
     *
     * @param value non-null table snapshot
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public LetterBuilder table(TableBlock value) {
        return block(value);
    }

    /**
     * Appends an advanced model block without leaving the fluent API. Its explicit style fields
     * override document defaults; detailed validation occurs at generation.
     *
     * @param value non-null body block
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public LetterBuilder block(Block value) {
        body.add(ContentParts.blocks(List.of(ArgumentChecks.required(value, "block"))));
        return this;
    }

    /**
     * Appends an explicit softly kept group of immutable blocks.
     *
     * @param blocks non-null ordered child blocks
     * @return this builder
     */
    public LetterBuilder group(Block... blocks) {
        return block(GroupBlock.of(ArgumentChecks.required(blocks, "group blocks")));
    }

    /**
     * Appends grouped plain text using the ordinary newline/paragraph rules.
     *
     * @param content non-null text chunks without page breaks
     * @return this builder
     */
    public LetterBuilder group(String... content) {
        return block(new GroupBlock(PlainText.region(null, content), null));
    }

    /**
     * Replaces the header with plain text repeated on every page. Single newlines change lines and
     * blank lines separate paragraphs. The strings are copied; the final header style is applied
     * when built. An empty array clears the content.
     *
     * @param text non-null array of non-null strings without form-feed page breaks
     * @return this builder
     * @throws IllegalArgumentException if the array or a string is null, or a string contains a
     *     form-feed page break
     */
    public LetterBuilder header(String... text) {
        return header(PageSelector.ALL, text);
    }

    /**
     * Replaces header content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param text region content
     * @return this builder
     * @throws IllegalArgumentException if input or page selection is invalid
     */
    public LetterBuilder header(PageSelector pages, String... text) {
        PlainText.region(headerStyle, text);
        header.set(pages, ContentParts.text(Arrays.asList(text.clone())));
        return this;
    }

    /**
     * Replaces the footer with plain text repeated on every page. Single newlines change lines and
     * blank lines separate paragraphs. The strings are copied; the final footer style is applied
     * when built. An empty array clears the content.
     *
     * @param text non-null array of non-null strings without form-feed page breaks
     * @return this builder
     * @throws IllegalArgumentException if the array or a string is null, or a string contains a
     *     form-feed page break
     */
    public LetterBuilder footer(String... text) {
        return footer(PageSelector.ALL, text);
    }

    /**
     * Replaces footer content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param text region content
     * @return this builder
     * @throws IllegalArgumentException if input or page selection is invalid
     */
    public LetterBuilder footer(PageSelector pages, String... text) {
        PlainText.region(footerStyle, text);
        footer.set(pages, ContentParts.text(Arrays.asList(text.clone())));
        return this;
    }

    /**
     * Replaces header style overrides on top of zero before/after spacing and the currently
     * selected header alignment. Explicit block styles can override these inherited fields.
     *
     * @param value non-null header style overrides
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public LetterBuilder headerStyle(Style value) {
        Style alignment = Style.builder().textAlign(headerLayout.alignment()).build();
        headerStyle = alignment.overlay(ArgumentChecks.required(value, "header style"));
        return this;
    }

    /**
     * Applies appearance overrides to one header selection after the shared header defaults.
     *
     * @param pages non-null page selection
     * @param value non-null appearance overrides
     * @return this builder
     */
    public LetterBuilder headerStyle(PageSelector pages, Style value) {
        header.style(pages, value);
        return this;
    }

    /**
     * Applies appearance overrides to one footer selection after the shared footer defaults.
     *
     * @param pages non-null page selection
     * @param value non-null appearance overrides
     * @return this builder
     */
    public LetterBuilder footerStyle(PageSelector pages, Style value) {
        footer.style(pages, value);
        return this;
    }

    /**
     * Positions an automatic header box within the space beside the logo and supplies default
     * content alignment. Explicit block styles can override the content alignment. With exact
     * {@link #headerBoundsMm(double, double, double, double) bounds}, only content alignment
     * changes; the box origin stays fixed.
     *
     * @param alignment LEFT, CENTER or RIGHT
     * @return this builder
     * @throws IllegalArgumentException if alignment is null or JUSTIFY
     */
    public LetterBuilder headerAlignment(TextAlign alignment) {
        headerLayout = headerLayout.withAlignment(alignment);
        headerStyle = headerStyle.overlay(Style.builder().textAlign(alignment).build());
        return this;
    }

    /**
     * Replaces the header box width in millimeters. Automatic placement follows the selected
     * alignment; an exact box is resized without moving its origin. Available width beside the
     * final logo is checked when built. Without an explicit width, automatic headers fill the
     * available space.
     *
     * @param width finite positive header width in mm
     * @return this builder
     * @throws IllegalArgumentException if width is nonpositive or nonfinite
     */
    public LetterBuilder headerWidthMm(double width) {
        headerLayout = headerLayout.withWidth(width);
        return this;
    }

    /**
     * Replaces automatic placement with an exact header box measured from the inner page upper-left
     * corner. Add left/top margins to obtain paper-edge coordinates. The box bottom reserves body
     * space followed by a 5 mm gap. Later width/height calls retain this origin; page fit is
     * checked when built.
     *
     * @param x finite nonnegative horizontal offset from the left margin, in mm
     * @param y finite nonnegative vertical offset from the top margin, in mm
     * @param width finite positive box width in mm
     * @param height finite positive box height in mm
     * @return this builder
     * @throws IllegalArgumentException if offsets are negative/nonfinite or dimensions are
     *     nonpositive/nonfinite
     */
    public LetterBuilder headerBoundsMm(double x, double y, double width, double height) {
        headerLayout = headerLayout.withBounds(new Bounds(x, y, width, height));
        return this;
    }

    /**
     * Replaces footer overrides on top of the default 9 pt gray text and zero before/after spacing.
     * Explicit block styles can override the inherited fields.
     *
     * @param value non-null footer style overrides
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public LetterBuilder footerStyle(Style value) {
        footerStyle = ArgumentChecks.required(value, "footer style");
        return this;
    }

    /**
     * Replaces the header content-box height, initially 25 mm. An exact box keeps its origin.
     * Automatic layout also accommodates a taller logo. Images passed directly as bytes have an
     * initial fit at registration time and are refitted to the final box when built.
     *
     * @param value finite positive height in mm
     * @return this builder
     * @throws IllegalArgumentException if the value is nonpositive or nonfinite
     */
    public LetterBuilder headerHeightMm(double value) {
        headerLayout = headerLayout.withHeight(value);
        return this;
    }

    /**
     * Replaces footer content-box height, initially 15 mm. A nonempty footer reserves this height
     * plus a 5 mm gap from the body. Footer content is resolved against this final height.
     *
     * @param value finite positive height in mm
     * @return this builder
     * @throws IllegalArgumentException if the value is nonpositive or nonfinite
     */
    public LetterBuilder footerHeightMm(double value) {
        footerHeight = ArgumentChecks.positive(value, "footer height");
        return this;
    }

    /**
     * Replaces the logo by immediately decoding copied PNG/JPEG/static SVG bytes and fitting their
     * aspect ratio into a 45 by 20 mm box. The logo repeats on every page; the header is placed
     * beside it. A logo wider than the final inner page width is refitted at build time.
     *
     * @param image non-null readable PNG, JPEG or static SVG bytes
     * @return this builder
     * @throws IllegalArgumentException if bytes are null, unreadable or not PNG/JPEG/static SVG
     */
    public LetterBuilder logo(byte[] image) {
        logo =
                ImageBlocks.fit(
                        image,
                        LetterLayoutDefaults.LOGO_MAX_WIDTH_MM,
                        LetterLayoutDefaults.LOGO_MAX_HEIGHT_MM);
        return this;
    }

    /**
     * Reads and replaces the logo immediately, fitting it into a 45 by 20 mm box with its aspect
     * ratio preserved.
     *
     * @param image non-null path to a PNG, JPEG or static SVG file
     * @return this builder
     * @throws IOException if the file cannot be read
     * @throws NullPointerException if {@code image} is null
     * @throws IllegalArgumentException if the file is not a readable PNG, JPEG or static SVG image
     */
    public LetterBuilder logo(Path image) throws IOException {
        return logo(Files.readAllBytes(image));
    }

    /**
     * Replaces signature content with the supplied immutable parts stacked in argument order. Text
     * and image parts may be combined. Placement and reserved height are configured separately; the
     * default placement follows the body.
     *
     * @param parts non-null, nonempty array of non-null signature parts
     * @return this builder
     * @throws IllegalArgumentException if the array is null or empty, or a part is null
     */
    public LetterBuilder signature(SignatureContent... parts) {
        ArgumentChecks.required(parts, "signature parts");
        ArgumentChecks.require(parts.length > 0, "At least one signature part is required");
        var blocks = new ArrayList<Block>();
        Arrays.stream(parts)
                .forEach(
                        part ->
                                blocks.addAll(
                                        ArgumentChecks.required(part, "signature part").blocks()));
        signature = ContentParts.blocks(blocks);
        return this;
    }

    /**
     * Replaces signature placement. {@link SignaturePlacement#FLOW} follows the body and is the
     * default; {@link SignaturePlacement#BOTTOM_LAST} anchors above the footer on the final page
     * using the configured signature height.
     *
     * @param value non-null placement policy
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public LetterBuilder signaturePlacement(SignaturePlacement value) {
        signaturePlacement = ArgumentChecks.required(value, "signature placement");
        return this;
    }

    /**
     * Replaces the signature height, initially 20 mm. When content exists and {@link
     * SignaturePlacement#BOTTOM_LAST} is selected, this reserves the final-page signature box. FLOW
     * reserves no signature zone.
     *
     * <p>Signature HTML uses this height for default and percentage-based image sizes. Explicit
     * signature parts and typed blocks retain their own dimensions; resize an image part with
     * {@link com.nsdev.lettergenerator.signature.ImageSignature#sizeMm(double, double)}.
     *
     * @param value finite positive height in mm
     * @return this builder
     * @throws IllegalArgumentException if the value is nonpositive or nonfinite
     */
    public LetterBuilder signatureHeightMm(double value) {
        signatureHeight = ArgumentChecks.positive(value, "signature height");
        return this;
    }

    /**
     * Replaces the watermark with the default text-watermark configuration. Watermarks are applied
     * during PDF generation; text validity is checked there.
     *
     * @param text text for {@link TextWatermark#of(String)}
     * @return this builder
     */
    public LetterBuilder watermark(String text) {
        return watermark(TextWatermark.of(text));
    }

    /**
     * Replaces the watermark configuration for PDF post-processing. Content, opacity and geometry
     * are validated when generating the PDF.
     *
     * @param value non-null text or image watermark
     * @return this builder
     * @throws IllegalArgumentException if {@code value} is null
     */
    public LetterBuilder watermark(Watermark value) {
        watermark = ArgumentChecks.required(value, "watermark");
        return this;
    }

    /**
     * Appends an advanced fixed element using the configured header/footer zones. Its page selector
     * and bounds are preserved; collision and zone-fit checks occur during generation. Configure
     * suitable region heights for the supplied geometry.
     *
     * @param element non-null fixed element
     * @return this builder
     * @throws IllegalArgumentException if {@code element} is null
     */
    public LetterBuilder fixedElement(FixedElement element) {
        fixedElements.add(ArgumentChecks.required(element, "fixed element"));
        return this;
    }

    /**
     * Registers a defensive copy of named image bytes for later HTML asset resolution. Reusing a
     * name replaces its previous bytes. Image decoding and format checks occur when markup
     * referencing the asset is built, so registration can follow content calls.
     *
     * @param name nonblank asset identifier used by markup image references
     * @param data non-null image bytes; supported image formats are PNG, JPEG and static SVG
     * @return this builder
     * @throws IllegalArgumentException if the name is null or blank, or the bytes are null
     */
    public LetterBuilder asset(String name, byte[] data) {
        ArgumentChecks.require(name != null && !name.isBlank(), "Asset name must not be blank");
        assets.put(name, ArgumentChecks.required(data, "asset bytes").clone());
        return this;
    }

    /**
     * Reads a local file immediately and registers its bytes as a named markup image asset. Reusing
     * a name replaces the existing asset; image decoding is deferred until referenced markup is
     * built.
     *
     * @param name nonblank asset identifier
     * @param path non-null image file to read
     * @return this builder
     * @throws IOException if the file cannot be read
     * @throws NullPointerException if {@code path} is null
     * @throws IllegalArgumentException if {@code name} is null or blank
     */
    public LetterBuilder asset(String name, Path path) throws IOException {
        return asset(name, Files.readAllBytes(path));
    }

    /**
     * Appends body content as a fragment of the supported HTML subset. Parsing and named asset
     * resolution are deferred to build time, using final page/region dimensions. Assets may be
     * registered before or after this call.
     *
     * @param markup non-null supported HTML fragment
     * @return this builder
     * @throws IllegalArgumentException if {@code markup} is null
     */
    public LetterBuilder bodyHtml(String markup) {
        body.add(ContentParts.html(ArgumentChecks.required(markup, "body markup")));
        return this;
    }

    /**
     * Replaces header content with a fragment of the supported HTML subset. Parsing and named asset
     * resolution are deferred to build time, using final page/region dimensions. Assets may be
     * registered before or after this call. Page breaks are rejected in this region.
     *
     * @param markup non-null supported HTML fragment
     * @return this builder
     * @throws IllegalArgumentException if {@code markup} is null
     */
    public LetterBuilder headerHtml(String markup) {
        return headerHtml(PageSelector.ALL, markup);
    }

    /**
     * Replaces header content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param markup region content
     * @return this builder
     * @throws IllegalArgumentException if input or page selection is invalid
     */
    public LetterBuilder headerHtml(PageSelector pages, String markup) {
        header.set(pages, ContentParts.html(ArgumentChecks.required(markup, "header markup")));
        return this;
    }

    /**
     * Replaces footer content with a fragment of the supported HTML subset. Parsing and named asset
     * resolution are deferred to build time, using final page/region dimensions. Assets may be
     * registered before or after this call. Page breaks are rejected in this region.
     *
     * @param markup non-null supported HTML fragment
     * @return this builder
     * @throws IllegalArgumentException if {@code markup} is null
     */
    public LetterBuilder footerHtml(String markup) {
        return footerHtml(PageSelector.ALL, markup);
    }

    /**
     * Replaces footer content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param markup region content
     * @return this builder
     * @throws IllegalArgumentException if input or page selection is invalid
     */
    public LetterBuilder footerHtml(PageSelector pages, String markup) {
        footer.set(pages, ContentParts.html(ArgumentChecks.required(markup, "footer markup")));
        return this;
    }

    /**
     * Replaces signature content with a fragment of the supported HTML subset. Parsing and named
     * asset resolution are deferred to build time, using final page/region dimensions. Assets may
     * be registered before or after this call. Page breaks are rejected in this region.
     *
     * @param markup non-null supported HTML fragment
     * @return this builder
     * @throws IllegalArgumentException if {@code markup} is null
     */
    public LetterBuilder signatureHtml(String markup) {
        signature = ContentParts.html(ArgumentChecks.required(markup, "signature markup"));
        return this;
    }

    /**
     * Replaces header content with an ordered snapshot of advanced model blocks. An empty array
     * clears the content. The final region defaults are overlaid when built; explicit block style
     * fields take precedence. Region restrictions and layout are validated at build or generation
     * time.
     *
     * @param blocks non-null array of non-null blocks
     * @return this builder
     * @throws IllegalArgumentException if the array is null
     * @throws NullPointerException if any block is null
     */
    public LetterBuilder headerBlocks(Block... blocks) {
        return headerBlocks(PageSelector.ALL, blocks);
    }

    /**
     * Replaces header content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param blocks region content
     * @return this builder
     * @throws IllegalArgumentException if input or page selection is invalid
     */
    public LetterBuilder headerBlocks(PageSelector pages, Block... blocks) {
        header.set(
                pages,
                ContentParts.blocks(
                        Arrays.asList(ArgumentChecks.required(blocks, "header blocks"))));
        return this;
    }

    /**
     * Replaces footer content with an ordered snapshot of advanced model blocks. An empty array
     * clears the content. The final region defaults are overlaid when built; explicit block style
     * fields take precedence. Region restrictions and layout are validated at build or generation
     * time.
     *
     * @param blocks non-null array of non-null blocks
     * @return this builder
     * @throws IllegalArgumentException if the array is null
     * @throws NullPointerException if any block is null
     */
    public LetterBuilder footerBlocks(Block... blocks) {
        return footerBlocks(PageSelector.ALL, blocks);
    }

    /**
     * Replaces footer content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param blocks region content
     * @return this builder
     * @throws IllegalArgumentException if input or page selection is invalid
     */
    public LetterBuilder footerBlocks(PageSelector pages, Block... blocks) {
        footer.set(
                pages,
                ContentParts.blocks(
                        Arrays.asList(ArgumentChecks.required(blocks, "footer blocks"))));
        return this;
    }

    /**
     * Replaces signature content with an ordered snapshot of advanced model blocks. An empty array
     * clears the content. The blocks retain their own style settings. Region restrictions and
     * layout are validated at build or generation time.
     *
     * @param blocks non-null array of non-null blocks
     * @return this builder
     * @throws IllegalArgumentException if the array is null
     * @throws NullPointerException if any block is null
     */
    public LetterBuilder signatureBlocks(Block... blocks) {
        signature =
                ContentParts.blocks(
                        Arrays.asList(ArgumentChecks.required(blocks, "signature blocks")));
        return this;
    }

    /**
     * Replaces the header with a repeated PNG/JPEG/static SVG image. Bytes are copied and decoded
     * immediately, preserving aspect ratio inside a 160 mm wide box with the current header height
     * (default 25 mm). The image is refitted to final region dimensions when built.
     *
     * @param image non-null readable PNG, JPEG or static SVG bytes
     * @return this builder
     * @throws IllegalArgumentException if bytes are null, unreadable or not PNG/JPEG/static SVG
     */
    public LetterBuilder header(byte[] image) {
        return header(PageSelector.ALL, image);
    }

    /**
     * Replaces header content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param image region content
     * @return this builder
     * @throws IllegalArgumentException if input or page selection is invalid
     */
    public LetterBuilder header(PageSelector pages, byte[] image) {
        header.set(
                pages,
                ContentParts.image(
                        ImageBlocks.fit(
                                image, DEFAULT_REGION_IMAGE_WIDTH_MM, headerLayout.heightMm())));
        return this;
    }

    /**
     * Reads a PNG/JPEG/static SVG file immediately and replaces the header with its fitted image.
     * See {@link #header(byte[])} for sizing and final fitting behavior.
     *
     * @param image non-null path to the header image
     * @return this builder
     * @throws IOException if the file cannot be read
     * @throws NullPointerException if {@code image} is null
     * @throws IllegalArgumentException if the file is not a readable PNG, JPEG or static SVG image
     */
    public LetterBuilder header(Path image) throws IOException {
        return header(PageSelector.ALL, image);
    }

    /**
     * Replaces header content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param image region content
     * @return this builder
     * @throws IOException if image bytes cannot be read
     */
    public LetterBuilder header(PageSelector pages, Path image) throws IOException {
        return header(pages, Files.readAllBytes(image));
    }

    /**
     * Replaces the footer with a repeated PNG/JPEG/static SVG image. Bytes are copied and decoded
     * immediately, preserving aspect ratio inside a 160 mm wide box with the current footer height
     * (default 15 mm). The image is refitted to final region dimensions when built.
     *
     * @param image non-null readable PNG, JPEG or static SVG bytes
     * @return this builder
     * @throws IllegalArgumentException if bytes are null, unreadable or not PNG/JPEG/static SVG
     */
    public LetterBuilder footer(byte[] image) {
        return footer(PageSelector.ALL, image);
    }

    /**
     * Replaces footer content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param image region content
     * @return this builder
     * @throws IllegalArgumentException if input or page selection is invalid
     */
    public LetterBuilder footer(PageSelector pages, byte[] image) {
        footer.set(
                pages,
                ContentParts.image(
                        ImageBlocks.fit(image, DEFAULT_REGION_IMAGE_WIDTH_MM, footerHeight)));
        return this;
    }

    /**
     * Reads a PNG/JPEG/static SVG file immediately and replaces the footer with its fitted image.
     * See {@link #footer(byte[])} for sizing and final fitting behavior.
     *
     * @param image non-null path to the footer image
     * @return this builder
     * @throws IOException if the file cannot be read
     * @throws NullPointerException if {@code image} is null
     * @throws IllegalArgumentException if the file is not a readable PNG, JPEG or static SVG image
     */
    public LetterBuilder footer(Path image) throws IOException {
        return footer(PageSelector.ALL, image);
    }

    /**
     * Replaces footer content for one page selection; ALL is fallback for unspecified selections.
     * Geometry and inherited region defaults are shared across variants. Empty text/block content
     * suppresses the fallback on that selection. Paths are read immediately; other inputs retain
     * the corresponding ordinary overload's validation and snapshot rules.
     *
     * @param pages non-null page selection
     * @param image region content
     * @return this builder
     * @throws IOException if image bytes cannot be read
     */
    public LetterBuilder footer(PageSelector pages, Path image) throws IOException {
        return footer(pages, Files.readAllBytes(image));
    }

    /**
     * Replaces the logo with an advanced image block retaining its supplied dimensions. If wider
     * than the final inner page, its bytes are refitted during building. Detailed image and style
     * validation is performed during PDF generation.
     *
     * @param image non-null image block
     * @return this builder
     * @throws IllegalArgumentException if {@code image} is null
     */
    public LetterBuilder logo(ImageBlock image) {
        logo = ArgumentChecks.required(image, "logo");
        return this;
    }

    /**
     * Resolves deferred HTML, assets, images and region geometry into an immutable snapshot. Body
     * blocks remain in append order, and header/logo/footer space is reserved before creating the
     * page specification. This does not render a PDF or validate every advanced model constraint:
     * pass the result to {@link com.nsdev.lettergenerator.LetterPdfGenerator}. Later builder
     * changes do not modify the returned document.
     *
     * @return a new independent immutable document
     * @throws IllegalStateException if no body content was supplied or resolved
     * @throws IllegalArgumentException if page/region geometry is invalid
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if deferred markup or a
     *     referenced image asset is invalid
     */
    public LetterDocument build() {
        if (body.isEmpty()) {
            throw new IllegalStateException("A letter needs body content");
        }
        PageSpec physicalPage = new PageSpec(page, margins, 0d, 0d, 0d, dimensions, orientation);
        double width =
                ArgumentChecks.positive(
                        physicalPage.widthMm() - margins.left() - margins.right(),
                        "available page width");
        double height =
                ArgumentChecks.positive(
                        physicalPage.heightMm() - margins.top() - margins.bottom(),
                        "available page height");
        ImageBlock fittedLogo = logo;
        if (fittedLogo != null && fittedLogo.widthMm() > width) {
            fittedLogo = ImageBlocks.fit(fittedLogo, width, fittedLogo.heightMm());
        }
        Bounds headerBounds = headerLayout.resolve(width, fittedLogo);
        var bodyBlocks = new ArrayList<Block>();
        for (ContentPart part : body) {
            bodyBlocks.addAll(part.resolve(assets, width, height, true, null, theme));
        }
        if (bodyBlocks.isEmpty()) {
            throw new IllegalStateException("A letter needs body content");
        }
        List<Block> orderedBodyBlocks = letterParts.assemble(bodyBlocks, width, settings.locale());
        var layout =
                LetterRegions.arrange(
                        physicalPage,
                        margins,
                        header.resolve(
                                assets,
                                headerBounds.width(),
                                headerBounds.height(),
                                (theme == null
                                        ? DEFAULT_HEADER_STYLE.overlay(headerStyle)
                                        : headerStyle),
                                theme == null ? null : DEFAULT_HEADER_STYLE,
                                theme),
                        footer.resolve(
                                assets,
                                width,
                                footerHeight,
                                theme == null
                                        ? DEFAULT_FOOTER_STYLE.overlay(footerStyle)
                                        : footerStyle,
                                theme == null ? null : DEFAULT_FOOTER_STYLE,
                                theme),
                        fittedLogo,
                        headerBounds,
                        footerHeight,
                        fixedElements);
        Signature builtSignature =
                signature == null
                        ? null
                        : new Signature(
                                signaturePlacement,
                                signature.resolve(
                                        assets, width, signatureHeight, false, null, theme),
                                signaturePlacement == SignaturePlacement.BOTTOM_LAST
                                        ? new Bounds(0, 0, width, signatureHeight)
                                        : null);
        PageSpec arrangedPage = layout.page();
        double signatureZone =
                builtSignature != null && signaturePlacement == SignaturePlacement.BOTTOM_LAST
                        ? signatureHeight
                        : 0;
        PageSpec builtPage =
                new PageSpec(
                        arrangedPage.size(),
                        arrangedPage.marginsMm(),
                        arrangedPage.topZoneMm(),
                        arrangedPage.bottomZoneMm(),
                        signatureZone,
                        arrangedPage.dimensions(),
                        arrangedPage.orientation());
        return new LetterDocument(
                LetterDocument.SUPPORTED_SCHEMA_VERSION,
                builtPage,
                theme == null ? DEFAULT_BODY_STYLE.overlay(style) : style,
                List.of(
                        new Section(
                                BODY_SECTION_NAME,
                                null,
                                theme == null
                                        ? orderedBodyBlocks
                                        : List.of(
                                                new GroupBlock(
                                                        orderedBodyBlocks,
                                                        Style.builder()
                                                                .keepTogether(Keep.AUTO)
                                                                .build(),
                                                        DEFAULT_BODY_STYLE)))),
                layout.elements(),
                builtSignature,
                watermark,
                theme,
                components,
                settings,
                attachments);
    }
}
