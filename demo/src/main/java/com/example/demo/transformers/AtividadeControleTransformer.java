package com.example.demo.transformers;

import java.util.*;

public class AtividadeControleTransformer {

    /**
     * Transforma JSON de atividades de controle em formato Excel.
     * Aba "ETAPA 5. ATIVIDADES DE CONTROLE" com campos de tratamento de riscos.
     */
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object contentObj = input.get("content");

        if (contentObj instanceof List<?> content) {
            for (Object item : content) {
                if (item instanceof Map<?, ?> atividade) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    
                    // Extrai risco aninhado
                    Object riscoObj = atividade.get("risco");
                    
                    // Seção Plano de Tratamento
                    row.put("Evento de Risco", extrairRiscoNome(riscoObj));
                    row.put("Opção de Tratamento", extrairOpcaoTratamento(riscoObj));
                    row.put("Responsável pelo Tratamento", val(atividade.get("responsavelTratamento")));
                    row.put("Data prevista para início da implementação", val(atividade.get("dataInicio")));
                    row.put("Data prevista para o fim da implementação", val(atividade.get("dataTermino")));
                    row.put("Status", val(atividade.get("statusImplementacao")));
                    row.put("Ações preventivas (descrever)", val(atividade.get("gatilho")));
                    row.put("Monitoramento", ""); // Campo vazio para preenchimento
                    
                    // Seção Plano de Contingência
                    row.put("Gatilho (descrever)", val(atividade.get("gatilho")));
                    row.put("Ações de Contingência (descrever)", val(atividade.get("acoesContingencia")));
                    row.put("Responsável", val(atividade.get("responsavelContingencia")));
                    
                    rows.add(row);
                }
            }
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 5. ATIVIDADES DE CONTROLE", rows);
        return result;
    }

    private static String extrairRiscoNome(Object riscoObj) {
        if (riscoObj instanceof Map<?, ?> risco) {
            return val(risco.get("nome"));
        }
        return "";
    }

    private static String extrairOpcaoTratamento(Object riscoObj) {
        if (riscoObj instanceof Map<?, ?> risco) {
            Object respostaObj = risco.get("respostaRisco");
            if (respostaObj instanceof Map<?, ?> resposta) {
                return val(resposta.get("opcaoTratamento"));
            }
        }
        return "";
    }

    private static String val(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
