package com.example.demo;

import java.util.*;

public class AvaliacaoRiscoDataTransformer {

    /**
     * Transforma JSON de avaliação de riscos em formato Excel.
     * Aba "ETAPA 3. AVALIAÇÃO DE RISCOS" com probabilidade, impacto e controles.
     */
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object contentObj = input.get("content");

        if (contentObj instanceof List<?> content) {
            for (Object item : content) {
                if (item instanceof Map<?, ?> avaliacao) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    
                    // Extrai risco aninhado
                    Object riscoObj = avaliacao.get("risco");
                    
                    // Ordem e nomes conforme imagem
                    row.put("Evento de Risco", extrairRiscoNome(riscoObj));
                    row.put("Probabilidade", val(avaliacao.get("probabilidade")));
                    row.put("P", ""); // Coluna vazia (será calculada no Excel se necessário)
                    row.put("Impacto", val(avaliacao.get("impacto")));
                    row.put("I", ""); // Coluna vazia
                    row.put("Risco Inerente (PxI)", calcularNivelRisco(avaliacao.get("probabilidade"), avaliacao.get("impacto")));
                    row.put("Classificação do Risco Inerente", classificarRisco(avaliacao.get("probabilidade"), avaliacao.get("impacto")));
                    row.put("Controles Preventivos (descrever)", val(avaliacao.get("controlesPreventivos")));
                    row.put("Controles de Atenuação e recuperação (descrever)", val(avaliacao.get("controlesAtenuacao")));
                    row.put("Avaliação dos Controles", ""); // Campo vazio para preenchimento
                    row.put("FAC", val(avaliacao.get("fac")));
                    row.put("Risco Residual", calcularNivelResidual(avaliacao.get("probabilidade"), avaliacao.get("impacto"), avaliacao.get("fac")));
                    row.put("Classificação do Risco Residual", classificarRiscoResidual(avaliacao.get("probabilidade"), avaliacao.get("impacto"), avaliacao.get("fac")));
                    row.put("Data da Última Avaliação", val(avaliacao.get("dataUltimaAvaliacao")));
                    
                    rows.add(row);
                }
            }
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 3. AVALIAÇÃO DE RISCOS", rows);
        return result;
    }

    private static String extrairRiscoNome(Object riscoObj) {
        if (riscoObj instanceof Map<?, ?> risco) {
            return val(risco.get("nome"));
        }
        return "";
    }

    private static String calcularNivelRisco(Object probObj, Object impactoObj) {
        try {
            int prob = Integer.parseInt(String.valueOf(probObj));
            int impacto = Integer.parseInt(String.valueOf(impactoObj));
            int nivel = prob * impacto;
            return String.valueOf(nivel);
        } catch (Exception e) {
            return "";
        }
    }

    private static String classificarRisco(Object probObj, Object impactoObj) {
        try {
            int prob = Integer.parseInt(String.valueOf(probObj));
            int impacto = Integer.parseInt(String.valueOf(impactoObj));
            int nivel = prob * impacto;
            
            if (nivel <= 25) return "BAIXO";
            else if (nivel <= 50) return "MÉDIO";
            else if (nivel <= 75) return "ALTO";
            else return "CRÍTICO";
        } catch (Exception e) {
            return "";
        }
    }

    private static String calcularNivelResidual(Object probObj, Object impactoObj, Object facObj) {
        try {
            int prob = Integer.parseInt(String.valueOf(probObj));
            int impacto = Integer.parseInt(String.valueOf(impactoObj));
            double fac = Double.parseDouble(String.valueOf(facObj));
            double residual = prob * impacto * fac;
            return String.format("%.1f", residual);
        } catch (Exception e) {
            return "";
        }
    }

    private static String classificarRiscoResidual(Object probObj, Object impactoObj, Object facObj) {
        try {
            int prob = Integer.parseInt(String.valueOf(probObj));
            int impacto = Integer.parseInt(String.valueOf(impactoObj));
            double fac = Double.parseDouble(String.valueOf(facObj));
            double residual = prob * impacto * fac;
            
            if (residual <= 12.5) return "BAIXO";
            else if (residual <= 25) return "MÉDIO";
            else if (residual <= 37.5) return "ALTO";
            else return "CRÍTICO";
        } catch (Exception e) {
            return "";
        }
    }

    private static String val(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
