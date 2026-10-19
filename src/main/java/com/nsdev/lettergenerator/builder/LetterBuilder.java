package com.nsdev.lettergenerator.builder;

import com.nsdev.lettergenerator.internal.builder.HeaderLayout;
import com.nsdev.lettergenerator.internal.builder.LetterRegions;
import com.nsdev.lettergenerator.internal.builder.content.ContentPart;
import com.nsdev.lettergenerator.internal.builder.content.ContentParts;
import com.nsdev.lettergenerator.internal.content.ImageBlocks;
import com.nsdev.lettergenerator.internal.content.PlainText;
import com.nsdev.lettergenerator.internal.defaults.LetterLayoutDefaults;
import com.nsdev.lettergenerator.internal.util.ArgumentChecks;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.PageBreakBlock;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import com.nsdev.lettergenerator.model.text.TextRun;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import com.nsdev.lettergenerator.signature.SignatureContent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /** Owned image byte copies available to all deferred markup fragments. */
    private final Map<String, byte[]> assets = new LinkedHashMap<>();

    /** Additional advanced fixed elements appended after generated region elements. */
    private final List<FixedElement> fixedElements = new ArrayList<>();

    /** Current paper dimensions; region geometry is calculated only at build time. */
    private PageSize page = PageSize.A4;

    /** Current page-edge margins in millimeters. */
    private Margins margins = Margins.defaults();

    /** Current body defaults, including the baseline paragraph gap. */
    private Style style = DEFAULT_BODY_STYLE;

    /** Current header typography and alignment defaults applied during content resolution. */
    private Style headerStyle = DEFAULT_HEADER_STYLE;

    /** Current footer typography defaults applied during content resolution. */
    private Style footerStyle = DEFAULT_FOOTER_STYLE;

    /** Latest header content; an empty part produces no repeating header element. */
    private ContentPart header = ContentParts.text(List.of());

    /** Latest footer content; an empty part produces no repeating footer element. */
    private ContentPart footer = ContentParts.text(List.of());

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
     * Replaces the paper size, initially {@link PageSize#A4}. Header geometry and image fitting use
     * the selected size when built.
     *
     * @param size non-null paper size
     * @return this builder
     * @throws IllegalArgumentException if {@code size} is null
     */
    public LetterBuilder page(PageSize size) {
        page = ArgumentChecks.required(size, "page size");
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
        style = DEFAULT_BODY_STYLE.overlay(ArgumentChecks.required(value, "style"));
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
        PlainText.region(headerStyle, text);
        header = ContentParts.text(Arrays.asList(text.clone()));
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
        PlainText.region(footerStyle, text);
        footer = ContentParts.text(Arrays.asList(text.clone()));
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
        headerStyle =
                DEFAULT_HEADER_STYLE
                        .overlay(alignment)
                        .overlay(ArgumentChecks.required(value, "header style"));
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
        footerStyle = DEFAULT_FOOTER_STYLE.overlay(ArgumentChecks.required(value, "footer style"));
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
     * Replaces the logo by immediately decoding copied PNG/JPEG bytes and fitting their aspect
     * ratio into a 45 by 20 mm box. The logo repeats on every page; the header is placed beside it.
     * A logo wider than the final inner page width is refitted at build time.
     *
     * @param image non-null readable PNG or JPEG bytes
     * @return this builder
     * @throws IllegalArgumentException if bytes are null, unreadable or not PNG/JPEG
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
     * @param image non-null path to a PNG or JPEG file
     * @return this builder
     * @throws IOException if the file cannot be read
     * @throws NullPointerException if {@code image} is null
     * @throws IllegalArgumentException if the file is not a readable PNG or JPEG image
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
     * @param data non-null image bytes; supported image formats are PNG and JPEG
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
        header = ContentParts.html(ArgumentChecks.required(markup, "header markup"));
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
        footer = ContentParts.html(ArgumentChecks.required(markup, "footer markup"));
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
        header =
                ContentParts.blocks(
                        Arrays.asList(ArgumentChecks.required(blocks, "header blocks")));
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
        footer =
                ContentParts.blocks(
                        Arrays.asList(ArgumentChecks.required(blocks, "footer blocks")));
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
     * Replaces the header with a repeated PNG/JPEG image. Bytes are copied and decoded immediately,
     * preserving aspect ratio inside a 160 mm wide box with the current header height (default 25
     * mm). The image is refitted to final region dimensions when built.
     *
     * @param image non-null readable PNG or JPEG bytes
     * @return this builder
     * @throws IllegalArgumentException if bytes are null, unreadable or not PNG/JPEG
     */
    public LetterBuilder header(byte[] image) {
        header =
                ContentParts.image(
                        ImageBlocks.fit(
                                image, DEFAULT_REGION_IMAGE_WIDTH_MM, headerLayout.heightMm()));
        return this;
    }

    /**
     * Reads a PNG/JPEG file immediately and replaces the header with its fitted image. See {@link
     * #header(byte[])} for sizing and final fitting behavior.
     *
     * @param image non-null path to the header image
     * @return this builder
     * @throws IOException if the file cannot be read
     * @throws NullPointerException if {@code image} is null
     * @throws IllegalArgumentException if the file is not a readable PNG or JPEG image
     */
    public LetterBuilder header(Path image) throws IOException {
        return header(Files.readAllBytes(image));
    }

    /**
     * Replaces the footer with a repeated PNG/JPEG image. Bytes are copied and decoded immediately,
     * preserving aspect ratio inside a 160 mm wide box with the current footer height (default 15
     * mm). The image is refitted to final region dimensions when built.
     *
     * @param image non-null readable PNG or JPEG bytes
     * @return this builder
     * @throws IllegalArgumentException if bytes are null, unreadable or not PNG/JPEG
     */
    public LetterBuilder footer(byte[] image) {
        footer =
                ContentParts.image(
                        ImageBlocks.fit(image, DEFAULT_REGION_IMAGE_WIDTH_MM, footerHeight));
        return this;
    }

    /**
     * Reads a PNG/JPEG file immediately and replaces the footer with its fitted image. See {@link
     * #footer(byte[])} for sizing and final fitting behavior.
     *
     * @param image non-null path to the footer image
     * @return this builder
     * @throws IOException if the file cannot be read
     * @throws NullPointerException if {@code image} is null
     * @throws IllegalArgumentException if the file is not a readable PNG or JPEG image
     */
    public LetterBuilder footer(Path image) throws IOException {
        return footer(Files.readAllBytes(image));
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
        double width =
                ArgumentChecks.positive(
                        page.widthMm() - margins.left() - margins.right(), "available page width");
        double height =
                ArgumentChecks.positive(
                        page.heightMm() - margins.top() - margins.bottom(),
                        "available page height");
        ImageBlock fittedLogo = logo;
        if (fittedLogo != null && fittedLogo.widthMm() > width) {
            fittedLogo = ImageBlocks.fit(fittedLogo, width, fittedLogo.heightMm());
        }
        Bounds headerBounds = headerLayout.resolve(width, fittedLogo);
        var bodyBlocks = new ArrayList<Block>();
        for (ContentPart part : body) {
            bodyBlocks.addAll(part.resolve(assets, width, height, true, null));
        }
        if (bodyBlocks.isEmpty()) {
            throw new IllegalStateException("A letter needs body content");
        }
        var layout =
                LetterRegions.arrange(
                        page,
                        margins,
                        header.resolve(
                                assets,
                                headerBounds.width(),
                                headerBounds.height(),
                                false,
                                headerStyle),
                        footer.resolve(assets, width, footerHeight, false, footerStyle),
                        fittedLogo,
                        headerBounds,
                        footerHeight,
                        fixedElements);
        Signature builtSignature =
                signature == null
                        ? null
                        : new Signature(
                                signaturePlacement,
                                signature.resolve(assets, width, signatureHeight, false, null),
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
                        signatureZone);
        return new LetterDocument(
                LetterDocument.SUPPORTED_SCHEMA_VERSION,
                builtPage,
                style,
                List.of(new Section(BODY_SECTION_NAME, null, bodyBlocks)),
                layout.elements(),
                builtSignature,
                watermark);
    }
}
