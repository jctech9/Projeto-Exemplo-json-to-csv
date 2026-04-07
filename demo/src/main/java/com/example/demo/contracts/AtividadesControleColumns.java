package com.example.demo.contracts;

import java.util.List;

public enum AtividadesControleColumns {
    EVENTO_RISCO(0, "eventoRisco", "Evento de Risco", "Evento de Risco", 15000, true),
    OPCAO_TRATAMENTO(1, "opcaoTratamento", "Opção de Tratamento", "Opção de\nTratamento", 6000, false),
    RESPONSAVEL_TRATAMENTO(
            2,
            "responsavelTratamento",
            "Responsável pelo Tratamento",
            "Responsável pelo\nTratamento",
            7000,
            false
    ),
    DATA_INICIO(
            3,
            "dataPrevistaInicioImplementacao",
            "Data prevista para início da implementação",
            "Data prevista para início\nda implementação",
            7000,
            false
    ),
    DATA_FIM(
            4,
            "dataPrevistaFimImplementacao",
            "Data prevista para o fim da implementação",
            "Data prevista para o fim\nda implementação",
            7000,
            false
    ),
    STATUS(5, "status", "Status", "Status", 6000, false),
    ACOES_PREVENTIVAS(
            6,
            "acoesPreventivas",
            "Ações preventivas (descrever)",
            "Ações preventivas\n(descrever)",
            15000,
            true
    ),
    MONITORAMENTO(7, "monitoramento", "Monitoramento", "Monitoramento", 15000, true),
    GATILHO(8, "gatilho", "Gatilho (descrever)", "Gatilho\n(descrever)", 15000, true),
    ACOES_CONTINGENCIA(
            9,
            "acoesContingencia",
            "Ações de Contingência (descrever)",
            "Ações de Contingência\n(descrever)",
            15000,
            true
    ),
    RESPONSAVEL_CONTINGENCIA(10, "responsavelContingencia", "Responsável", "Responsável", 7000, false);

    private final int index;
    private final String key;
    private final String header;
    private final String headerLabel;
    private final int columnWidth;
    private final boolean leftAligned;

    AtividadesControleColumns(
            int index,
            String key,
            String header,
            String headerLabel,
            int columnWidth,
            boolean leftAligned
    ) {
        this.index = index;
        this.key = key;
        this.header = header;
        this.headerLabel = headerLabel;
        this.columnWidth = columnWidth;
        this.leftAligned = leftAligned;
    }

    public int index() {
        return index;
    }

    public String key() {
        return key;
    }

    public String header() {
        return header;
    }

    public String headerLabel() {
        return headerLabel;
    }

    public int columnWidth() {
        return columnWidth;
    }

    public boolean leftAligned() {
        return leftAligned;
    }

    public static List<AtividadesControleColumns> ordered() {
        return List.of(values());
    }
}
