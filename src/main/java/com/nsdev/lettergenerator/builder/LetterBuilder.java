package com.nsdev.lettergenerator.builder;

import com.nsdev.lettergenerator.internal.builder.*;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.*;
import com.nsdev.lettergenerator.model.layout.*;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import com.nsdev.lettergenerator.model.text.TextRun;
import com.nsdev.lettergenerator.model.watermark.TextWatermark;
import com.nsdev.lettergenerator.model.watermark.Watermark;
import com.nsdev.lettergenerator.signature.SignatureContent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Mutable builder for ordinary application letters. Content calls append in order; configuration
 * calls replace the corresponding option. Each build returns an independent immutable document.
 */
public final class LetterBuilder {
    private static final double DEFAULT_FOOTER_HEIGHT_MM = 15.0;
    private static final double DEFAULT_LOGO_WIDTH_MM = 45.0;
    private static final double DEFAULT_LOGO_HEIGHT_MM = 20.0;
    private static final double DEFAULT_PARAGRAPH_GAP_MM = 3.0;
    private static final double DEFAULT_FOOTER_SIZE_PT = 9.0;
    private static final String DEFAULT_FOOTER_COLOR = "#666666";
    private static final String BODY_SECTION_NAME = "body";
    private static final Style DEFAULT_BODY_STYLE =
            Style.builder().spaceAfterMm(DEFAULT_PARAGRAPH_GAP_MM).build();
    private static final Style DEFAULT_HEADER_STYLE =
            Style.builder().spaceBeforeMm(0).spaceAfterMm(0).build();
    private static final Style DEFAULT_FOOTER_STYLE =
            DEFAULT_HEADER_STYLE.overlay(
                    Style.builder()
                            .fontSizePt(DEFAULT_FOOTER_SIZE_PT)
                            .color(DEFAULT_FOOTER_COLOR)
                            .build());
    private static final double DEFAULT_REGION_IMAGE_WIDTH_MM = 160.0;
    private static final double DEFAULT_SIGNATURE_HEIGHT_MM = 20.0;
    private final List<ContentPart> body = new ArrayList<>();
    private final Map<String, byte[]> assets = new LinkedHashMap<>();
    private final List<FixedElement> fixedElements = new ArrayList<>();
    private PageSize page = PageSize.A4;
    private Margins margins = Margins.defaults();
    private Style style = DEFAULT_BODY_STYLE;
    private Style headerStyle = DEFAULT_HEADER_STYLE;
    private Style footerStyle = DEFAULT_FOOTER_STYLE;
    private ContentPart header = new ContentPart.Text(List.of());
    private ContentPart footer = new ContentPart.Text(List.of());
    private ImageBlock logo;
    private HeaderLayout headerLayout = HeaderLayout.defaults();
    private double footerHeight = DEFAULT_FOOTER_HEIGHT_MM;
    private ContentPart signature;
    private Watermark watermark;

    public LetterBuilder page(PageSize size) {
        page = BuilderChecks.required(size, "page size");
        return this;
    }

    public LetterBuilder marginsMm(double allSides) {
        return margins(Margins.uniform(BuilderChecks.nonnegative(allSides, "margins")));
    }

    public LetterBuilder margins(Margins value) {
        margins = BuilderChecks.required(value, "margins");
        return this;
    }

    public LetterBuilder style(Style value) {
        style = DEFAULT_BODY_STYLE.overlay(BuilderChecks.required(value, "style"));
        return this;
    }

    /** Single newlines change lines; blank lines separate paragraphs; form feeds change pages. */
    public LetterBuilder body(String... content) {
        body.add(new ContentPart.Blocks(PlainText.body(content)));
        return this;
    }

    public LetterBuilder heading(String text) {
        return block(
                new Heading(
                        List.of(new TextRun(BuilderChecks.required(text, "heading"), null, null)),
                        null));
    }

    public LetterBuilder pageBreak() {
        return block(new PageBreakBlock());
    }

    public LetterBuilder table(TableBlock value) {
        return block(value);
    }

    /** Appends an advanced model block without leaving the fluent API. */
    public LetterBuilder block(Block value) {
        body.add(new ContentPart.Blocks(List.of(BuilderChecks.required(value, "block"))));
        return this;
    }

    public LetterBuilder header(String... text) {
        PlainText.region(headerStyle, text);
        header = new ContentPart.Text(Arrays.asList(text.clone()));
        return this;
    }

    public LetterBuilder footer(String... text) {
        PlainText.region(footerStyle, text);
        footer = new ContentPart.Text(Arrays.asList(text.clone()));
        return this;
    }

    public LetterBuilder headerStyle(Style value) {
        Style alignment = Style.builder().textAlign(headerLayout.alignment()).build();
        headerStyle =
                DEFAULT_HEADER_STYLE
                        .overlay(alignment)
                        .overlay(BuilderChecks.required(value, "header style"));
        return this;
    }

    /**
     * Positions an automatic header box LEFT, CENTER or RIGHT in the space beside the logo and
     * supplies that default content alignment. Explicit block styles can override it. With exact
     * bounds, only the default content alignment changes; the box origin stays fixed.
     */
    public LetterBuilder headerAlignment(TextAlign alignment) {
        headerLayout = headerLayout.withAlignment(alignment);
        headerStyle = headerStyle.overlay(Style.builder().textAlign(alignment).build());
        return this;
    }

    /**
     * Sets a positive header box width in mm. Automatic placement follows the selected alignment;
     * an exact box is resized without moving its origin. Final page/asset settings resolve at
     * build.
     */
    public LetterBuilder headerWidthMm(double width) {
        headerLayout = headerLayout.withWidth(width);
        return this;
    }

    /**
     * Sets exact header bounds in mm, relative to the inner page's upper-left corner. Add the left
     * and top margins to convert to paper-edge coordinates. The header's bottom reserves body space
     * automatically, followed by the standard gap. Later width/height calls retain this x/y origin.
     */
    public LetterBuilder headerBoundsMm(double x, double y, double width, double height) {
        headerLayout = headerLayout.withBounds(new Bounds(x, y, width, height));
        return this;
    }

    public LetterBuilder footerStyle(Style value) {
        footerStyle = DEFAULT_FOOTER_STYLE.overlay(BuilderChecks.required(value, "footer style"));
        return this;
    }

    /** Sets a positive header content height in mm; resizes an exact box at its existing origin. */
    public LetterBuilder headerHeightMm(double value) {
        headerLayout = headerLayout.withHeight(value);
        return this;
    }

    public LetterBuilder footerHeightMm(double value) {
        footerHeight = BuilderChecks.positive(value, "footer height");
        return this;
    }

    public LetterBuilder logo(byte[] image) {
        logo = ImageBlocks.fit(image, DEFAULT_LOGO_WIDTH_MM, DEFAULT_LOGO_HEIGHT_MM);
        return this;
    }

    public LetterBuilder logo(Path image) throws IOException {
        return logo(Files.readAllBytes(image));
    }

    public LetterBuilder signature(SignatureContent... parts) {
        BuilderChecks.required(parts, "signature parts");
        BuilderChecks.require(parts.length > 0, "At least one signature part is required");
        var blocks = new ArrayList<Block>();
        Arrays.stream(parts)
                .forEach(
                        part ->
                                blocks.addAll(
                                        BuilderChecks.required(part, "signature part").blocks()));
        signature = new ContentPart.Blocks(blocks);
        return this;
    }

    public LetterBuilder watermark(String text) {
        return watermark(TextWatermark.of(text));
    }

    public LetterBuilder watermark(Watermark value) {
        watermark = BuilderChecks.required(value, "watermark");
        return this;
    }

    /** Advanced fixed elements share the configured header/footer zones and collision checks. */
    public LetterBuilder fixedElement(FixedElement element) {
        fixedElements.add(BuilderChecks.required(element, "fixed element"));
        return this;
    }

    public LetterBuilder asset(String name, byte[] data) {
        BuilderChecks.require(name != null && !name.isBlank(), "Asset name must not be blank");
        assets.put(name, BuilderChecks.required(data, "asset bytes").clone());
        return this;
    }

    public LetterBuilder asset(String name, Path path) throws IOException {
        return asset(name, Files.readAllBytes(path));
    }

    public LetterBuilder bodyHtml(String markup) {
        body.add(new ContentPart.Html(BuilderChecks.required(markup, "body markup")));
        return this;
    }

    public LetterBuilder headerHtml(String markup) {
        header = new ContentPart.Html(BuilderChecks.required(markup, "header markup"));
        return this;
    }

    public LetterBuilder footerHtml(String markup) {
        footer = new ContentPart.Html(BuilderChecks.required(markup, "footer markup"));
        return this;
    }

    public LetterBuilder signatureHtml(String markup) {
        signature = new ContentPart.Html(BuilderChecks.required(markup, "signature markup"));
        return this;
    }

    public LetterBuilder headerBlocks(Block... blocks) {
        header =
                new ContentPart.Blocks(
                        Arrays.asList(BuilderChecks.required(blocks, "header blocks")));
        return this;
    }

    public LetterBuilder footerBlocks(Block... blocks) {
        footer =
                new ContentPart.Blocks(
                        Arrays.asList(BuilderChecks.required(blocks, "footer blocks")));
        return this;
    }

    public LetterBuilder signatureBlocks(Block... blocks) {
        signature =
                new ContentPart.Blocks(
                        Arrays.asList(BuilderChecks.required(blocks, "signature blocks")));
        return this;
    }

    public LetterBuilder header(byte[] image) {
        header =
                new ContentPart.Image(
                        ImageBlocks.fit(
                                image, DEFAULT_REGION_IMAGE_WIDTH_MM, headerLayout.heightMm()));
        return this;
    }

    public LetterBuilder header(Path image) throws IOException {
        return header(Files.readAllBytes(image));
    }

    public LetterBuilder footer(byte[] image) {
        footer =
                new ContentPart.Image(
                        ImageBlocks.fit(image, DEFAULT_REGION_IMAGE_WIDTH_MM, footerHeight));
        return this;
    }

    public LetterBuilder footer(Path image) throws IOException {
        return footer(Files.readAllBytes(image));
    }

    public LetterBuilder logo(ImageBlock image) {
        logo = BuilderChecks.required(image, "logo");
        return this;
    }

    public LetterDocument build() {
        if (body.isEmpty()) {
            throw new IllegalStateException("A letter needs body content");
        }
        double width =
                BuilderChecks.positive(
                        page.widthMm() - margins.left() - margins.right(), "available page width");
        double height =
                BuilderChecks.positive(
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
                                SignaturePlacement.FLOW,
                                signature.resolve(
                                        assets, width, DEFAULT_SIGNATURE_HEIGHT_MM, false, null),
                                null);
        return new LetterDocument(
                LetterDocument.SUPPORTED_SCHEMA_VERSION,
                layout.page(),
                style,
                List.of(new Section(BODY_SECTION_NAME, null, bodyBlocks)),
                layout.elements(),
                builtSignature,
                watermark);
    }
}
