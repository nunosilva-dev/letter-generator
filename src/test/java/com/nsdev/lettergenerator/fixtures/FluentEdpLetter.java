package com.nsdev.lettergenerator.fixtures;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.Table;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.signature.TextSignature;
import java.io.IOException;

/**
 * Application-style example: content and a few visual overrides, with no paragraph or box records.
 */
public final class FluentEdpLetter {
    private FluentEdpLetter() {}

    public static LetterDocument document() throws IOException {
        return Letter.builder()
                .marginsMm(10)
                .style(
                        Style.builder()
                                .fontFamily(EdpReferenceLetter.FONT_FAMILY)
                                .fontSizePt(9.3)
                                .spaceAfterMm(2.5)
                                .build())
                .logo(EdpReferenceLetter.logo())
                .header("ANA EXEMPLO\nRUA DA ENERGIA, 24\n1000-001 LISBOA")
                .headerStyle(Style.builder().fontSizePt(8).build())
                .headerHeightMm(55)
                .body("Caro/a cliente,")
                .heading("O preço do seu contrato de eletricidade vai ser atualizado em 2024")
                .body(
                        "A EDP Comercial vai baixar a sua componente de preço, denominada Energia e Estrutura Comercial.",
                        "Esta redução entra em vigor a 1 de janeiro e representa, em média, para os clientes residenciais da EDP Comercial, uma descida de 15% na componente de preço definida pela EDP Comercial. Pode consultar os valores atualizados desta componente na tabela abaixo.",
                        "Sobre esta alteração, deverá ainda ser considerado o impacto na sua fatura de eletricidade da atualização das Tarifas de Acesso às Redes (TAR), que serão definidas pela Entidade Reguladora dos Serviços Energéticos (ERSE) durante o mês de dezembro, com entrada em vigor a partir de 1 de janeiro, e que poderá consultar posteriormente na página de perguntas frequentes em edp.pt ou junto dos canais habituais de apoio ao cliente.",
                        "Comunicamos também que os descontos de que dispõe vão manter-se e passam a incidir sobre este novo preço, tal como previsto nas suas condições contratuais.",
                        "Informamos que, caso não aceite as novas condições, pode opor-se à renovação do seu contrato no prazo de 30 dias, sem quaisquer encargos.")
                .heading("Conheça a sua nova componente de preço Energia e Estrutura Comercial")
                .body("Potência 3,45 kVA | Opção Simples")
                .table(
                        Table.builder(
                                        "Energia e Estrutura Comercial",
                                        "Até 31 de dezembro de 2023",
                                        "A partir de 1 de janeiro de 2024")
                                .headerStyle(
                                        Style.builder()
                                                .fontSizePt(8)
                                                .color("#98BC6A")
                                                .bold(false)
                                                .build())
                                .style(Style.builder().fontSizePt(8.5).build())
                                .row("Energia (€/kWh)", "0,2008", "0,1632")
                                .row("Potência (€/dia)", "0,0645", "0,0815")
                                .build())
                .body(
                        "Nota: os preços apresentados apenas refletem a componente Energia e Estrutura Comercial, à qual acrescem as TAR definidas pela ERSE.",
                        "CPE (Código de Ponto de Entrega): PT00 0000 0000 0000 0000 XX")
                .heading("Caso tenha alguma questão, não hesite em falar connosco")
                .body(
                        "Para esclarecer questões, fale connosco através da área de apoio ao cliente em edp.pt, da app EDP, da linha de atendimento 213 53 53 53 (dias úteis das 9h às 20h | chamada para a rede fixa nacional), ou das nossas lojas.")
                .signature(
                        TextSignature.of(
                                "Com os melhores cumprimentos,\n\nDuarte Bettencourt\nDiretor Comercial"))
                .footer("Exemplo com dados fictícios")
                .footerHeightMm(5)
                .build();
    }
}
