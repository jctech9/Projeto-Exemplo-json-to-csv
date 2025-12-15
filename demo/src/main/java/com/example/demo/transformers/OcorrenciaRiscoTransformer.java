package com.example.demo.transformers;

import java.util.*;

public class OcorrenciaRiscoTransformer {

    /**
     * Transforma JSON de ocorrências de risco em formato Excel.
     * Aba "OCORRÊNCIAS DE RISCO" com 6 colunas:
     * Evento de Risco, Data da Ocorrência, Descrição da Ocorrência, 
     * Responsável pela Solução, Solução, Resultados
     */
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object contentObj = input.get("content");

        if (contentObj instanceof List<?> content) {
            for (Object item : content) {
                if (item instanceof Map<?, ?> ocorrencia) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    
                    // Buscar o evento de risco relacionado
                    row.put("Evento de Risco", extrairEventoRisco(ocorrencia));
                    row.put("Data da Ocorrência", val(ocorrencia.get("dataOcorrencia")));
                    row.put("Descrição da Ocorrência", val(ocorrencia.get("descricao")));
                    row.put("Responsável pela Solução", val(ocorrencia.get("responsavelSolucao")));
                    row.put("Solução", val(ocorrencia.get("solucao")));
                    row.put("Resultados", val(ocorrencia.get("resultados")));
                    
                    rows.add(row);
                }
            }
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("OCORRÊNCIAS DE RISCO", rows);
        return result;
    }

    private static String extrairEventoRisco(Map<?, ?> ocorrencia) {
        // Tentar extrair de diferentes campos possíveis
        Object eventoRiscoObj = ocorrencia.get("eventoRisco");
        if (eventoRiscoObj instanceof Map<?, ?> eventoRiscoMap) {
            String evento = val(eventoRiscoMap.get("evento"));
            if (!evento.isEmpty()) return evento;
        }
        
        Object riscoObj = ocorrencia.get("risco");
        if (riscoObj instanceof Map<?, ?> riscoMap) {
            String evento = val(riscoMap.get("evento"));
            if (!evento.isEmpty()) return evento;
            // fallback adicional: alguns riscos usam "nome" em vez de "evento"
            String nome = val(riscoMap.get("nome"));
            if (!nome.isEmpty()) return nome;
        }
        
        // fallback final: usa a própria descrição da ocorrência
        return val(ocorrencia.get("descricao"));
    }

    private static String val(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
