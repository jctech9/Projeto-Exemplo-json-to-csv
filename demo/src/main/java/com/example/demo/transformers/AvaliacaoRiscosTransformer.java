package com.example.demo.transformers;

import static com.example.demo.transformers.TransformerUtils.formatDateBr;
import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.val;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AvaliacaoRiscosTransformer {

    private AvaliacaoRiscosTransformer() {
    }

    // Aba ETAPA 3: Probabilidade, Impacto e cálculos de risco
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> avaliacao : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();

            // Preenchido por formula no service da ETAPA 3, referenciando a ETAPA 2.
            row.put("Evento de Risco", "");
            row.put("Probabilidade", mapearProbabilidade(avaliacao.get("probabilidade")));
            row.put("P", "");
            row.put("Impacto", mapearImpacto(avaliacao.get("impacto")));
            row.put("I", "");
            row.put("Risco Inerente (PxI)", "");
            row.put("Classificação do Risco Inerente", "");
            row.put("Controles Preventivos (descrever)", val(avaliacao.get("controlesPreventivos")));
            row.put("Controles de Atenuação e recuperação (descrever)", val(avaliacao.get("controlesAtenuacao")));
            row.put("Avaliação dos Controles", mapearAvaliacaoControles(avaliacao.get("fac")));
            row.put("FAC", "");
            row.put("Risco Residual", "");
            row.put("Classificação do Risco Residual", "");
            row.put("Data da Última Avaliação", formatDateBr(avaliacao.get("dataUltimaAvaliacao")));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 3. AVALIAÇÃO DE RISCOS", rows);
        return result;
    }

    // Mapeia valor numérico da probabilidade para texto descritivo
    private static String mapearProbabilidade(Object probObj) {
        Integer prob = parseEscalaInteira(probObj, "probabilidade");
        if (prob == null) return "";

        return switch (prob) {
            case 1 -> "Muito baixa";
            case 2 -> "Baixa";
            case 5 -> "Média";
            case 8 -> "Alta";
            case 10 -> "Muito alta";
            default -> throw new IllegalArgumentException("Valor de probabilidade invalido: " + probObj);
        };
    }

    // Mapeia valor numérico do impacto para texto descritivo
    private static String mapearImpacto(Object impactoObj) {
        Integer impacto = parseEscalaInteira(impactoObj, "impacto");
        if (impacto == null) return "";

        return switch (impacto) {
            case 1 -> "Muito baixo";
            case 2 -> "Baixo";
            case 5 -> "Médio";
            case 8 -> "Alto";
            case 10 -> "Muito alto";
            default -> throw new IllegalArgumentException("Valor de impacto invalido: " + impactoObj);
        };
    }

    // Mapeia o valor FAC para avaliação dos controles
    private static String mapearAvaliacaoControles(Object facObj) {
        Double fac = parseFac(facObj);
        if (fac == null) return "";

        if (fac < 0 || fac > 1) {
            throw new IllegalArgumentException("Valor de fac invalido: " + facObj);
        }

        if (fac <= 0.2) return "Forte";
        if (fac <= 0.4) return "Satisfatório";
        if (fac <= 0.6) return "Mediano";
        if (fac <= 0.8) return "Fraco";
        return "Inexistente";
    }

    private static Integer parseEscalaInteira(Object rawValue, String fieldName) {
        if (rawValue == null) return null;

        if (rawValue instanceof Number n) {
            double asDouble = n.doubleValue();
            int asInt = (int) asDouble;
            if (Double.compare(asDouble, (double) asInt) != 0) {
                throw new IllegalArgumentException("Formato invalido para " + fieldName + ": " + rawValue);
            }
            return asInt;
        }

        String raw = String.valueOf(rawValue).trim();
        if (raw.isEmpty()) return null;

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato invalido para " + fieldName + ": " + rawValue, e);
        }
    }

    private static Double parseFac(Object facObj) {
        if (facObj == null) return null;

        if (facObj instanceof Number n) {
            double fac = n.doubleValue();
            if (Double.isNaN(fac) || Double.isInfinite(fac)) {
                throw new IllegalArgumentException("Formato invalido para fac: " + facObj);
            }
            return fac;
        }

        String raw = String.valueOf(facObj).trim();
        if (raw.isEmpty()) return null;

        try {
            return Double.parseDouble(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato invalido para fac: " + facObj, e);
        }
    }
}
