package com.nsdev.lettergenerator.fixtures;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.Table;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.layout.SignaturePlacement;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import com.nsdev.lettergenerator.model.text.TextRun;
import com.nsdev.lettergenerator.signature.TextSignature;
import java.io.IOException;
import java.util.List;

/**
 * Fluent reference example: content calls and layout options, without fixed-zone records or HTML.
 */
public final class FluentEnergyLetter {
    private FluentEnergyLetter() {}

    public static LetterDocument document() throws IOException {
        return Letter.builder()
                .marginsMm(10)
                .style(
                        Style.builder()
                                .fontFamily(EnergyLetterContent.FONT_FAMILY)
                                .fontSizePt(9)
                                .color("#222222")
                                .lineHeight(1.2)
                                .spaceAfterMm(2)
                                .build())
                .logo(new ImageBlock(EnergyReferenceLetter.logo(), "image/png", 55d, 22d, null))
                .header(EnergyLetterContent.RECIPIENT)
                .headerBoundsMm(110, 20, 80, 35)
                .headerAlignment(TextAlign.RIGHT)
                .headerStyle(Style.builder().fontSizePt(8).build())
                .body(EnergyLetterContent.SALUTATION)
                .heading(EnergyLetterContent.TITLE)
                .body(
                        EnergyLetterContent.REDUCTION,
                        EnergyLetterContent.DISCOUNT,
                        EnergyLetterContent.TARIFFS,
                        EnergyLetterContent.DISCOUNTS,
                        EnergyLetterContent.CANCELLATION)
                .heading(EnergyLetterContent.COMPONENT_TITLE)
                .body(EnergyLetterContent.POWER)
                .table(
                        Table.builder(
                                        EnergyLetterContent.COMPONENT_HEADER,
                                        EnergyLetterContent.OLD_PRICE_HEADER,
                                        EnergyLetterContent.NEW_PRICE_HEADER)
                                .headerStyle(
                                        Style.builder()
                                                .fontSizePt(8)
                                                .color("#98BC6A")
                                                .bold(false)
                                                .textAlign(TextAlign.CENTER)
                                                .build())
                                .style(
                                        Style.builder()
                                                .fontSizePt(8.5)
                                                .lineHeight(1.15)
                                                .spaceAfterMm(2)
                                                .build())
                                .row("Energia (€/kWh)", "0,2008", "0,1632")
                                .row("Potência (€/dia)", "0,0645", "0,0815")
                                .build())
                .body(EnergyLetterContent.NOTE, EnergyLetterContent.DELIVERY_POINT)
                .heading(EnergyLetterContent.SUPPORT_TITLE)
                .body(EnergyLetterContent.SUPPORT)
                .signaturePlacement(SignaturePlacement.BOTTOM_LAST)
                .signatureHeightMm(25)
                .signature(
                        TextSignature.of(EnergyLetterContent.CLOSING)
                                .style(
                                        Style.builder()
                                                .fontSizePt(8)
                                                .spaceBeforeMm(0)
                                                .spaceAfterMm(1)
                                                .build()),
                        TextSignature.of(
                                        EnergyLetterContent.SIGNER_INSET
                                                + EnergyLetterContent.SIGNER)
                                .style(
                                        Style.builder()
                                                .fontFamily("Times")
                                                .fontSizePt(16)
                                                .italic()
                                                .lineHeight(1.1)
                                                .spaceBeforeMm(0)
                                                .spaceAfterMm(1)
                                                .build()),
                        TextSignature.of(EnergyLetterContent.SIGNER_DETAILS)
                                .style(
                                        Style.builder()
                                                .fontSizePt(8)
                                                .spaceBeforeMm(0)
                                                .spaceAfterMm(0)
                                                .build()))
                .footerBlocks(
                        new ImageBlock(
                                EnergyReferenceLetter.banner(),
                                "image/png",
                                190d,
                                5d,
                                Style.builder()
                                        .textAlign(TextAlign.CENTER)
                                        .spaceBeforeMm(0)
                                        .spaceAfterMm(0)
                                        .build()),
                        new Paragraph(
                                List.of(new TextRun(EnergyLetterContent.FOOTER, null, null)),
                                Style.builder()
                                        .fontSizePt(6)
                                        .color("#666666")
                                        .textAlign(TextAlign.CENTER)
                                        .spaceBeforeMm(1)
                                        .spaceAfterMm(0)
                                        .build()))
                .footerHeightMm(14)
                .build();
    }
}
