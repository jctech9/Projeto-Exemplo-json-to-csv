package com.example.demo;

import java.util.*;

public class ProcessoDataTransformer {

    /**
     * Transforma o JSON em formato compatível com Excel.
     * Aba "ETAPA 1. DADOS DO PROCESSO" com colunas: Processo, Objetivos, Unidade, Responsável.
     */
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object contentObj = input.get("content");

        if (contentObj instanceof List<?> content) {
            for (Object item : content) {
                if (item instanceof Map<?, ?> processo) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("Processo", val(processo.get("nome")));
                    row.put("Objetivos do Processo (Geral e específicos)", val(processo.get("objetivosGerais")));
                    
                    // Unidade
                    Object unidade = processo.get("unidadeOrganizacional");
                    if (unidade instanceof Map<?, ?> u) {
                        String sigla = val(((Map<?, ?>) u).get("sigla"));
                        String nome = val(((Map<?, ?>) u).get("nome"));
                        row.put("Unidade", sigla.isEmpty() ? nome : sigla);
                    } else {
                        row.put("Unidade", "");
                    }
                    
                    // Responsável
                    Object responsavel = processo.get("responsavel");
                    if (responsavel instanceof Map<?, ?> r) {
                        row.put("Responsável", val(((Map<?, ?>) r).get("nome")));
                    } else {
                        row.put("Responsável", "");
                    }
                    
                    rows.add(row);
                }
            }
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 1. DADOS DO PROCESSO", rows);
        return result;
    }

    private static String val(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
