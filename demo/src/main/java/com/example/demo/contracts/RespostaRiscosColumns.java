package com.example.demo.contracts;

import java.util.List;

public enum RespostaRiscosColumns {
    PROCESSO(0, "processo", "Processo", "Processo", 9000, true),
    FASE(1, "fase", "Fase", "Fase", 6000, false),
    EVENTO_RISCO(2, "eventoRisco", "Evento de Risco", "Evento de Risco", 22000, true),
    OPCAO_TRATAMENTO(3, "opcaoTratamento", "Opção de Tratamento", "Opção de Tratamento", 7000, false),
    JUSTIFICATIVA_TRATAMENTO(
            4,
            "justificativaOpcaoTratamento",
            "Justificativa da escolha da opção de tratamento",
            "Justificativa da escolha da opção de tratamento",
            50000,
            true
    );

    private final int index;
    private final String key;
    private final String header;
    private final String headerLabel;
    private final int columnWidth;
    private final boolean leftAligned;

    RespostaRiscosColumns(
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

    public static List<RespostaRiscosColumns> ordered() {
        return List.of(values());
    }

    public static int lastIndex() {
        return JUSTIFICATIVA_TRATAMENTO.index;
    }
}
