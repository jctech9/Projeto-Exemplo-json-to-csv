package com.example.demo.transformers;

import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.getNestedString;
import static com.example.demo.transformers.TransformerUtils.val;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AvaliacaoRiscosTransformer {

    private static final DateTimeFormatter FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Aba ETAPA 3: Probabilidade, Impacto e cálculos de risco
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> avaliacao : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();

            row.put("Evento de Risco", getNestedString(avaliacao, "risco", "nome"));
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
            row.put("Data da Última Avaliação", formatarData(avaliacao.get("dataUltimaAvaliacao")));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 3. AVALIAÇÃO DE RISCOS", rows);
        return result;
    }

    // Mapeia valor numérico da probabilidade para texto descritivo
    private static String mapearProbabilidade(Object probObj) {
        try {
            int prob = Integer.parseInt(String.valueOf(probObj));
            switch (prob) {
                case 1: return "Muito baixa";
                case 2: return "Baixa";
                case 5: return "Média";
                case 8: return "Alta";
                case 10: return "Muito alta";
                default: return String.valueOf(prob);
            }
        } catch (Exception e) {
            return "";
        }
    }

    // Mapeia valor numérico do impacto para texto descritivo
    private static String mapearImpacto(Object impactoObj) {
        try {
            int impacto = Integer.parseInt(String.valueOf(impactoObj));
            switch (impacto) {
                case 1: return "Muito baixo";
                case 2: return "Baixo";
                case 5: return "Médio";
                case 8: return "Alto";
                case 10: return "Muito alto";
                default: return String.valueOf(impacto);
            }
        } catch (Exception e) {
            return "";
        }
    }

    // Mapeia o valor FAC para avaliação dos controles
    private static String mapearAvaliacaoControles(Object facObj) {
        try {
            double fac = Double.parseDouble(String.valueOf(facObj));

            if (fac <= 0.2) return "Forte";
            else if (fac <= 0.4) return "Satisfatório";
            else if (fac <= 0.6) return "Mediano";
            else if (fac <= 0.8) return "Fraco";
            else return "Inexistente";
        } catch (Exception e) {
            return "";
        }
    }

    // Formata a data no padrão dd/MM/yyyy
    private static String formatarData(Object data) {
        if (data == null || data.toString().isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(data.toString());
            return date.format(FORMATTER_BR);
        } catch (Exception e) {
            return val(data);
        }
    }

}
