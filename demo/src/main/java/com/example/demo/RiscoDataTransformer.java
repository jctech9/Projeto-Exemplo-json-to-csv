package com.example.demo;

import java.util.*;

public class RiscoDataTransformer {

    /**
     * Transforma JSON de riscos em formato Excel.
     * Aba "ETAPA 2. IDENTIFICAÇÃO DE EVENTOS" com identificação e categorização de riscos.
     */
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object contentObj = input.get("content");

        if (contentObj instanceof List<?> content) {
            for (Object item : content) {
                if (item instanceof Map<?, ?> risco) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    
                    // Ordem das colunas conforme solicitado
                    row.put("Processo", extrairProcessoNome(risco.get("processo")));
                    row.put("Fase", val(risco.get("faseProcesso")));
                    row.put("Evento de Risco (indicar)", val(risco.get("nome")));
                    row.put("Tipo de Risco", val(risco.get("tipoRisco")));
                    row.put("Categoria", extrairCategoriaNome(risco.get("categoria")));
                    row.put("Tipo de Risco de Integridade", extrairTipoRiscoIntegridade(risco.get("tipoRiscoIntegridade")));
                    row.put("Causas (descrever)", val(risco.get("causa")));
                    row.put("Consequências (descrever)", val(risco.get("consequencias")));
                    
                    rows.add(row);
                }
            }
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 2. IDENTIFICAÇÃO DE EVENTOS", rows);
        return result;
    }

    private static String extrairProcessoNome(Object processoObj) {
        if (processoObj instanceof Map<?, ?> processo) {
            return val(processo.get("nome"));
        }
        return "";
    }

    private static String extrairCategoriaNome(Object categoriaObj) {
        if (categoriaObj instanceof Map<?, ?> categoria) {
            return val(categoria.get("nome"));
        }
        return "";
    }

    private static String extrairTipoRiscoIntegridade(Object tipoRiscoObj) {
        if (tipoRiscoObj instanceof Map<?, ?> tipoRisco) {
            return val(tipoRisco.get("nome"));
        }
        return "";
    }

    private static String val(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
