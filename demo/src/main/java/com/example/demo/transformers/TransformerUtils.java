package com.example.demo.transformers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TransformerUtils {
    private static final DateTimeFormatter FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TransformerUtils() {
        // utilitario, nao instanciar
    }

    // Retorna uma string vazia para valores nulos
    public static String val(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    // Converte um objeto em mapa
    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object v) {
        return v instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    // Retorna a lista content ja filtrada para Map.
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getContent(Map<String, Object> input) {
        if (input == null) return List.of();

        Object contentObj = input.get("content");
        if (!(contentObj instanceof List<?> list)) return List.of();

        List<Map<String, Object>> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) out.add((Map<String, Object>) m);
        }
        return out;
    }

    // Navegacao segura em mapas aninhados.
    @SuppressWarnings("unchecked")
    public static String getNestedString(Map<String, Object> map, String... path) {
        Object curr = map;
        for (String key : path) {
            if (!(curr instanceof Map<?, ?> m)) return "";
            curr = m.get(key);
        }
        return val(curr);
    }

    // Formata data no padrao dd/MM/yyyy.
    public static String formatDateBr(Object data) {
        if (data == null || data.toString().isBlank()) return "";
        try {
            LocalDate date = LocalDate.parse(data.toString());
            return date.format(FORMATTER_BR);
        } catch (Exception e) {
            return val(data); // retorna o valor original se nao conseguir converter
        }
    }
}
