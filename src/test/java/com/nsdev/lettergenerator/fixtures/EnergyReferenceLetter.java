package com.nsdev.lettergenerator.fixtures;

import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.layout.PageSpec;
import com.nsdev.lettergenerator.model.layout.Signature;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.layout.Zone;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.io.IOException;
import java.util.List;

/**
 * Typed-model recreation of the reference layout using synthetic identities and generic artwork.
 */
public final class EnergyReferenceLetter {
    public static final String FONT_FAMILY = EnergyLetterContent.FONT_FAMILY;

    private EnergyReferenceLetter() {}

    public static LetterDocument document() throws IOException {
        return new LetterDocument(
                1,
                new PageSpec(PageSize.A4, Margins.uniform(10), 60d, 19d, 25d),
                Style.builder()
                        .fontFamily(FONT_FAMILY)
                        .fontSizePt(9)
                        .color("#222222")
                        .lineHeight(1.2)
                        .spaceAfterMm(2)
                        .build(),
                List.of(new Section("body", null, body())),
                List.of(
                        new FixedElement(
                                "letter.logo",
                                Zone.TOP,
                                PageSelector.ALL,
                                new Bounds(0, 0, 55, 22),
                                List.of(new ImageBlock(logo(), "image/png", 55d, 22d, null))),
                        new FixedElement(
                                "letter.header",
                                Zone.TOP,
                                PageSelector.ALL,
                                new Bounds(110, 20, 80, 35),
                                List.of(
                                        paragraph(
                                                EnergyLetterContent.RECIPIENT,
                                                Style.builder()
                                                        .fontSizePt(8)
                                                        .textAlign(TextAlign.RIGHT)
                                                        .spaceBeforeMm(0)
                                                        .spaceAfterMm(0)
                                                        .build()))),
                        new FixedElement(
                                "letter.footer",
                                Zone.BOTTOM,
                                PageSelector.ALL,
                                new Bounds(0, 5, 190, 14),
                                List.of(
                                        new ImageBlock(
                                                banner(),
                                                "image/png",
                                                190d,
                                                5d,
                                                Style.builder()
                                                        .textAlign(TextAlign.CENTER)
                                                        .spaceBeforeMm(0)
                                                        .spaceAfterMm(0)
                                                        .build()),
                                        paragraph(
                                                EnergyLetterContent.FOOTER,
                                                Style.builder()
                                                        .fontSizePt(6)
                                                        .color("#666666")
                                                        .textAlign(TextAlign.CENTER)
                                                        .spaceBeforeMm(1)
                                                        .spaceAfterMm(0)
                                                        .build())))),
                new Signature(
                        SignaturePlacement.BOTTOM_LAST,
                        List.of(
                                paragraph(
                                        EnergyLetterContent.CLOSING,
                                        Style.builder()
                                                .fontSizePt(8)
                                                .spaceBeforeMm(0)
                                                .spaceAfterMm(1)
                                                .build()),
                                paragraph(
                                        EnergyLetterContent.SIGNER_INSET
                                                + EnergyLetterContent.SIGNER,
                                        Style.builder()
                                                .fontFamily("Times")
                                                .fontSizePt(16)
                                                .italic()
                                                .lineHeight(1.1)
                                                .spaceBeforeMm(0)
                                                .spaceAfterMm(1)
                                                .build()),
                                paragraph(
                                        EnergyLetterContent.SIGNER_DETAILS,
                                        Style.builder()
                                                .fontSizePt(8)
                                                .spaceBeforeMm(0)
                                                .spaceAfterMm(0)
                                                .build())),
                        new Bounds(0, 0, 190, 25)),
                null);
    }

    private static List<Block> body() {
        return List.of(
                paragraph(EnergyLetterContent.SALUTATION, null),
                heading(EnergyLetterContent.TITLE),
                paragraph(EnergyLetterContent.REDUCTION, null),
                paragraph(EnergyLetterContent.DISCOUNT, null),
                paragraph(EnergyLetterContent.TARIFFS, null),
                paragraph(EnergyLetterContent.DISCOUNTS, null),
                paragraph(EnergyLetterContent.CANCELLATION, null),
                heading(EnergyLetterContent.COMPONENT_TITLE),
                paragraph(EnergyLetterContent.POWER, null),
                prices(),
                paragraph(EnergyLetterContent.NOTE, null),
                paragraph(EnergyLetterContent.DELIVERY_POINT, null),
                heading(EnergyLetterContent.SUPPORT_TITLE),
                paragraph(EnergyLetterContent.SUPPORT, null));
    }

    private static TableBlock prices() {
        Style header =
                Style.builder()
                        .fontSizePt(8)
                        .color("#98BC6A")
                        .bold(false)
                        .textAlign(TextAlign.CENTER)
                        .build();
        return new TableBlock(
                List.of(1d, 1d, 1d),
                List.of(
                        cell(EnergyLetterContent.COMPONENT_HEADER, header, "#10384A"),
                        cell(EnergyLetterContent.OLD_PRICE_HEADER, header, "#10384A"),
                        cell(EnergyLetterContent.NEW_PRICE_HEADER, header, "#10384A")),
                List.of(
                        List.of(
                                cell("Energia (€/kWh)", null, null),
                                cell("0,2008", null, null),
                                cell("0,1632", null, null)),
                        List.of(
                                cell("Potência (€/dia)", null, null),
                                cell("0,0645", null, null),
                                cell("0,0815", null, null))),
                Style.builder().fontSizePt(8.5).lineHeight(1.15).spaceAfterMm(2).build());
    }

    private static TableCell cell(String text, Style style, String background) {
        return new TableCell(List.of(new TextRun(text, null, null)), style, background);
    }

    private static Paragraph paragraph(String text, Style style) {
        return new Paragraph(List.of(new TextRun(text, null, null)), style);
    }

    private static Heading heading(String text) {
        return new Heading(List.of(new TextRun(text, null, null)), null);
    }

    public static byte[] logo() throws IOException {
        return TestResources.bytes(TestResources.ENERGY_LOGO);
    }

    public static byte[] banner() throws IOException {
        return TestResources.bytes(TestResources.ENERGY_FOOTER);
    }
}
