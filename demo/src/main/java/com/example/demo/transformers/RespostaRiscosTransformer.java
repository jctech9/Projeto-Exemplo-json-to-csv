package com.example.demo.transformers;

import java.util.*;

public class RespostaRiscosTransformer {

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
                    
                    // Ordem conforme imagem: Processo, Fase, Evento de Risco, Opção de Tratamento, Justificativa
                    Object riscoObj = respostaRisco.get("risco");
                    if (riscoObj instanceof Map<?, ?> risco) {
                        // Processo associado
                        Object processoObj = risco.get("processo");
                        if (processoObj instanceof Map<?, ?> processo) {
                            row.put("Processo", val(processo.get("nome")));
                        } else {
                            row.put("Processo", "");
                        }
                        
                        // Fase
                        row.put("Fase", val(risco.get("faseProcesso")));
                        
                        // Evento de Risco
                        row.put("Evento de Risco", val(risco.get("nome")));
                        
                        // Opção de Tratamento
                        row.put("Opção de Tratamento", val(respostaRisco.get("opcaoTratamento")));
                        
                        // Justificativa da escolha da opção de tratamento
                        row.put("Justificativa da escolha da opção de tratamento", val(respostaRisco.get("justificativa")));
                        
                    } else {
                        row.put("Processo", "");
                        row.put("Fase", "");
                        row.put("Evento de Risco", "");
                        row.put("Opção de Tratamento", val(respostaRisco.get("opcaoTratamento")));
                        row.put("Justificativa da escolha da opção de tratamento", val(respostaRisco.get("justificativa")));
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
