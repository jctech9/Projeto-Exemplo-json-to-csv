package com.example.demo.contracts;

public enum SheetNames {
    ETAPA_1("ETAPA 1. DADOS DO PROCESSO", "ETAPA 1"),
    ETAPA_2("ETAPA 2. IDENTIF. DE EVENTOS", "ETAPA 2"),
    ETAPA_3("ETAPA 3. AVALIAÇÃO DE RISCOS", "ETAPA 3"),
    ETAPA_4("ETAPA 4. RESPOSTA AOS RISCOS", "ETAPA 4"),
    ETAPA_5("ETAPA 5. ATIVIDADES DE CONTROLE", "ETAPA 5"),
    OCORRENCIAS_RISCO("OCORRÊNCIAS DE RISCO", "OCORRENCIAS DE RISCO");

    private final String displayName;
    private final String marker;

    SheetNames(String displayName, String marker) {
        this.displayName = displayName;
        this.marker = marker;
    }

    public String displayName() {
        return displayName;
    }

    public String marker() {
        return marker;
    }
}
