package com.nsdev.lettergenerator.fixtures;

import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.layout.*;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Recreation of the supplied low-resolution letter, with fictional recipient/contract information.
 * Logo and typed italic signature are approximations; no screenshot is embedded in the output.
 */
public final class EdpReferenceLetter {
    public static final String FONT_FAMILY = "ReferenceSans";
    private static final String HEADER_BACKGROUND = "#10384A";
    private static final String HEADER_FOREGROUND = "#98BC6A";
    private static final String ROW_BACKGROUND = "#E9EAEA";
    private static final Style BOLD =
            new Style(null, null, null, true, null, null, null, null, null, null);
    private static final Style SMALL =
            new Style(null, 8d, null, null, null, null, null, 1.25, 0d, 2d);

    private EdpReferenceLetter() {}

    public static LetterDocument document() throws IOException {
        return new LetterDocument(
                1,
                new PageSpec(PageSize.A4, new Margins(10, 8, 10, 8), 62d, 3d, 0d),
                new Style(
                        FONT_FAMILY,
                        9.3,
                        "#222222",
                        false,
                        false,
                        false,
                        TextAlign.LEFT,
                        1.28,
                        0d,
                        2.6),
                List.of(new Section("electricity-contract-update", null, body())),
                List.of(
                        new FixedElement(
                                "logo",
                                Zone.TOP,
                                PageSelector.FIRST,
                                new Bounds(0, 2, 60, 24),
                                List.of(new ImageBlock(logo(), "image/png", 60d, 24d, null))),
                        new FixedElement(
                                "recipient",
                                Zone.TOP,
                                PageSelector.FIRST,
                                new Bounds(100, 27, 94, 30),
                                List.of(
                                        paragraph(
                                                "|| | |||| | | ||| || || ||| | ||||| || | |||",
                                                SMALL),
                                        paragraph(
                                                "ANA EXEMPLO\nRUA DA ENERGIA, 24\n1000-001 LISBOA",
                                                SMALL))),
                        new FixedElement(
                                "page",
                                Zone.BOTTOM,
                                PageSelector.ALL,
                                new Bounds(0, 0, 15, 3),
                                List.of(
                                        paragraph(
                                                "1/1",
                                                new Style(
                                                        null, 6d, "#777777", null, null, null, null,
                                                        1d, 0d, 0d))))),
                new Signature(
                        SignaturePlacement.FLOW,
                        List.of(
                                paragraph("Com os melhores cumprimentos,", SMALL),
                                paragraph(
                                        "Duarte Bettencourt",
                                        new Style(
                                                "Times", 16d, "#222222", false, true, false, null,
                                                1.1, 0d, 1d)),
                                paragraph("Duarte Bettencourt\nDiretor Comercial", SMALL)),
                        null),
                null);
    }

    private static List<Block> body() {
        return List.of(
                paragraph(
                        "Caro/a cliente,",
                        new Style("Times", 8.5, null, null, true, null, null, null, 0d, 5d)),
                paragraph(
                        "O preço do seu contrato de eletricidade vai ser atualizado em 2024 (Contrato 123456789)",
                        BOLD),
                rich(
                        "A EDP Comercial vai ",
                        "baixar a sua componente de preço",
                        ", denominada \"Energia e Estrutura Comercial\"."),
                rich(
                        "Esta redução entra em vigor a 1 de janeiro e representa, em média, para os clientes residenciais da EDP Comercial, uma ",
                        "descida de 15% na componente de preço definida pela EDP Comercial.",
                        " Pode consultar os valores atualizados desta componente na tabela abaixo."),
                paragraph(
                        "Sobre esta alteração, deverá ainda ser considerado o impacto na sua fatura de eletricidade da atualização das Tarifas de Acesso às Redes (TAR), que serão definidas pela Entidade Reguladora dos Serviços Energéticos (ERSE) durante o mês de dezembro, com entrada em vigor a partir de 1 de janeiro, e que poderá consultar posteriormente na página de perguntas frequentes em edp.pt ou junto dos canais habituais de apoio ao cliente.",
                        null),
                paragraph(
                        "Comunicamos também que os descontos de que dispõe vão manter-se e passam a incidir sobre este novo preço, tal como previsto nas suas condições contratuais.",
                        null),
                paragraph(
                        "Informamos que, caso não aceite as novas condições, pode opor-se à renovação do seu contrato no prazo de 30 dias, sem quaisquer encargos.",
                        null),
                paragraph(
                        "Conheça a sua nova componente de preço \"Energia e Estrutura Comercial\"",
                        BOLD),
                paragraph(
                        "Potência 3,45 kVA | Opção Simples",
                        new Style(null, 8.5, null, null, null, null, null, null, 0d, 1.5)),
                prices(),
                paragraph(
                        "Nota: os preços apresentados na tabela apenas refletem a componente de preço final definida pela EDP Comercial, denominada \"Energia e Estrutura Comercial\", não sendo diretamente comparáveis com os valores apresentados na fatura de eletricidade. A esta componente acrescem as TAR, a definir pela ERSE durante o mês de dezembro para entrada em vigor a 1 de janeiro, sobre as quais se poderá informar posteriormente junto dos nossos canais de atendimento.",
                        SMALL),
                paragraph("CPE (Código de Ponto de Entrega): PT00 0000 0000 0000 0000 XX", SMALL),
                paragraph("Caso tenha alguma questão, não hesite em falar connosco", BOLD),
                paragraph(
                        "Para esclarecer questões que possa ter sobre este ou outro tema, fale connosco através da área de apoio ao cliente em edp.pt, da app EDP, da linha de atendimento 213 53 53 53 (dias úteis das 9h às 20h | chamada para a rede fixa nacional), ou das nossas lojas.",
                        null));
    }

    private static TableBlock prices() {
        Style header =
                new Style(
                        null,
                        8.2,
                        HEADER_FOREGROUND,
                        false,
                        null,
                        null,
                        TextAlign.CENTER,
                        1.15,
                        null,
                        null);
        Style centered =
                new Style(null, null, null, null, null, null, TextAlign.CENTER, null, null, null);
        return new TableBlock(
                List.of(1d, 1d, 1d),
                List.of(
                        cell("Energia e Estrutura Comercial", header, HEADER_BACKGROUND),
                        cell(
                                "Valor em vigor até\n31 de dezembro de 2023",
                                header,
                                HEADER_BACKGROUND),
                        cell(
                                "Valor em vigor a partir de\n1 de janeiro de 2024",
                                header,
                                HEADER_BACKGROUND)),
                List.of(
                        List.of(
                                cell("Energia (€/kWh)", null, ROW_BACKGROUND),
                                cell("0,2008", centered, ROW_BACKGROUND),
                                cell("0,1632", centered, ROW_BACKGROUND)),
                        List.of(
                                cell("Potência (€/dia)", null, ROW_BACKGROUND),
                                cell("0,0645", centered, ROW_BACKGROUND),
                                cell("0,0815", centered, ROW_BACKGROUND))),
                new Style(null, 8.5, null, null, null, null, null, 1.15, 0d, 1.5));
    }

    private static TableCell cell(String text, Style style, String background) {
        return new TableCell(List.of(new TextRun(text, null, null)), style, background);
    }

    private static Paragraph paragraph(String text, Style style) {
        return new Paragraph(List.of(new TextRun(text, null, null)), style);
    }

    private static Paragraph rich(String before, String bold, String after) {
        return new Paragraph(
                List.of(
                        new TextRun(before, null, null),
                        new TextRun(bold, BOLD, null),
                        new TextRun(after, null, null)),
                null);
    }

    public static byte[] logo() throws IOException {
        var image = new BufferedImage(1000, 400, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color[] colors = {
                new Color(94, 76, 150), new Color(127, 184, 98), new Color(94, 181, 185)
            };
            for (int strand = 0; strand < colors.length; strand++) {
                var curve = new Path2D.Double();
                for (int step = 0; step <= 280; step++) {
                    double angle = step * Math.PI / 95 + strand * Math.PI * 2 / 3;
                    double radius = 150 * (1 - step / 320d);
                    double x = 180 + radius * Math.cos(angle);
                    double y = 190 + radius * Math.sin(angle);
                    if (step == 0) {
                        curve.moveTo(x, y);
                    } else {
                        curve.lineTo(x, y);
                    }
                }
                graphics.setColor(colors[strand]);
                graphics.setStroke(
                        new BasicStroke(17, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.draw(curve);
            }
            graphics.setColor(new Color(49, 65, 72));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 300));
            graphics.drawString("edp", 375, 285);
        } finally {
            graphics.dispose();
        }
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
