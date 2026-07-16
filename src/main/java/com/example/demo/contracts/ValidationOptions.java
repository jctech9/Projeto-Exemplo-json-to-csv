package com.example.demo.contracts;

public final class ValidationOptions {

    public static final String[] OPCOES_TRATAMENTO = {
            "Aceitar",
            "Mitigar",
            "Compartilhar",
            "Evitar"
    };

    public static final String[] STATUS_IMPLEMENTACAO = {
            "Não implementado",
            "Em implementação",
            "Implementado"
    };

    private ValidationOptions() {
    }

    public static String normalizeOption(String value, String[] options) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        for (String option : options) {
            if (option.equalsIgnoreCase(trimmed)) {
                return option;
            }
        }
        return trimmed;
    }

    public static String normalizeOpcaoTratamento(String value) {
        return normalizeOption(value, OPCOES_TRATAMENTO);
    }
}
