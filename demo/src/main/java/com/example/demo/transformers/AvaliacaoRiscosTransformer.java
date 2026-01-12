package com.example.demo.transformers;

import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.getNestedString;
import static com.example.demo.transformers.TransformerUtils.val;
import java.util.*;

public class AvaliacaoRiscosTransformer {

    // Aba ETAPA 3: Probabilidade, Impacto e cálculos de risco
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> avaliacao : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();

            row.put("Evento de Risco", getNestedString(avaliacao, "risco", "nome"));
            row.put("P", val(avaliacao.get("probabilidade"))); 
            row.put("Impacto", val(avaliacao.get("impacto")));
            row.put("I", val(avaliacao.get("impacto"))); 
            row.put("Risco Inerente (PxI)", calcularNivelRisco(avaliacao.get("probabilidade"), avaliacao.get("impacto")));
            row.put("Classificação do Risco Inerente", classificarRisco(avaliacao.get("probabilidade"), avaliacao.get("impacto")));
            row.put("Controles Preventivos (descrever)", val(avaliacao.get("controlesPreventivos")));
            row.put("Controles de Atenuação e recuperação (descrever)", val(avaliacao.get("controlesAtenuacao")));
            row.put("Avaliação dos Controles", mapearAvaliacaoControles(avaliacao.get("fac")));
            row.put("FAC", val(avaliacao.get("fac")));
            row.put("Risco Residual", calcularNivelResidual(avaliacao.get("probabilidade"), avaliacao.get("impacto"), avaliacao.get("fac")));
            row.put("Classificação do Risco Residual", classificarRiscoResidual(avaliacao.get("probabilidade"), avaliacao.get("impacto"), avaliacao.get("fac")));
            row.put("Data da Última Avaliação", val(avaliacao.get("dataUltimaAvaliacao")));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 3. AVALIAÇÃO DE RISCOS", rows);
        return result;
    }
    // Mapeia o valor FAC 
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
    // Calcula o nível de risco inerente (P x I)
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
    // Classifica o risco inerente com base no nível
    private static String classificarRisco(Object probObj, Object impactoObj) {
        try {
            int prob = Integer.parseInt(String.valueOf(probObj));
            int impacto = Integer.parseInt(String.valueOf(impactoObj));
            int nivel = prob * impacto;
            
            if (nivel < 10) return "BAIXO";
            else if (nivel < 40) return "MÉDIO";
            else if (nivel < 80) return "ALTO";
            else return "EXTREMO";
        } catch (Exception e) {
            return "";
        }
    }
    // Calcula o nível de risco residual (P x I x FAC)
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
    // Classifica o risco residual com base no nível
    private static String classificarRiscoResidual(Object probObj, Object impactoObj, Object facObj) {
        try {
            int prob = Integer.parseInt(String.valueOf(probObj));
            int impacto = Integer.parseInt(String.valueOf(impactoObj));
            double fac = Double.parseDouble(String.valueOf(facObj));
            double residual = prob * impacto * fac;
            
            if (residual < 10) return "BAIXO";
            else if (residual < 40) return "MÉDIO";
            else if (residual < 80) return "ALTO";
            else return "EXTREMO";
        } catch (Exception e) {
            return "";
        }
    }

}
