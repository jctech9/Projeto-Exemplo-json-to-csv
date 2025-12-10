package com.example.demo;

import java.util.*;

public class RespostaRiscoDataTransformer {

    /**
     * Transforma JSON de respostas a riscos em formato Excel.
     * Aba "ETAPA 4. RESPOSTA AOS RISCOS" com colunas principais do risco e resposta.
     */
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object contentObj = input.get("content");

        if (contentObj instanceof List<?> content) {
            for (Object item : content) {
                if (item instanceof Map<?, ?> respostaRisco) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    
                    // Ordem das colunas: Processo, Fase, Risco, Opção, Justificativa, demais
                    Object riscoObj = respostaRisco.get("risco");
                    if (riscoObj instanceof Map<?, ?> risco) {
                        // Processo associado
                        Object processoObj = risco.get("processo");
                        if (processoObj instanceof Map<?, ?> processo) {
                            row.put("Processo", val(processo.get("nome")));
                        } else {
                            row.put("Processo", "");
                        }
                        
                        // Fase do Processo
                        row.put("Fase do Processo", val(risco.get("faseProcesso")));
                        
                        // Risco
                        row.put("Risco", val(risco.get("nome")));
                        
                        // Opção de Tratamento e Justificativa
                        row.put("Opção de Tratamento", val(respostaRisco.get("opcaoTratamento")));
                        row.put("Justificativa", val(respostaRisco.get("justificativa")));
                        
                        // Demais campos
                        row.put("Tipo de Risco", val(risco.get("tipoRisco")));
                        row.put("Causa", val(risco.get("causa")));
                        row.put("Consequências", val(risco.get("consequencias")));
                        
                        // Categoria
                        Object categoriaObj = risco.get("categoria");
                        if (categoriaObj instanceof Map<?, ?> categoria) {
                            row.put("Categoria", val(categoria.get("nome")));
                        } else {
                            row.put("Categoria", "");
                        }
                    } else {
                        row.put("Processo", "");
                        row.put("Fase do Processo", "");
                        row.put("Risco", "");
                        row.put("Opção de Tratamento", val(respostaRisco.get("opcaoTratamento")));
                        row.put("Justificativa", val(respostaRisco.get("justificativa")));
                        row.put("Tipo de Risco", "");
                        row.put("Causa", "");
                        row.put("Consequências", "");
                        row.put("Categoria", "");
                    }
                    
                    rows.add(row);
                }
            }
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 4. RESPOSTA AOS RISCOS", rows);
        return result;
    }

    private static String val(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
