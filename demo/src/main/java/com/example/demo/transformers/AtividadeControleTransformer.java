package com.example.demo.transformers;

import static com.example.demo.transformers.TransformerUtils.asMap;
import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.val;
import java.util.*;

public class AtividadeControleTransformer {

    // Aba ETAPA 5: Plano de tratamento e contingência
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> atividade : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();

            Map<String, Object> risco = asMap(atividade.get("risco"));
            row.put("Evento de Risco", risco == null ? "" : val(risco.get("nome")));
            row.put("Opção de Tratamento", extrairOpcaoTratamento(risco));
            row.put("Responsável pelo Tratamento", val(atividade.get("responsavelTratamento")));
            row.put("Data prevista para início da implementação", val(atividade.get("dataInicio")));
            row.put("Data prevista para o fim da implementação", val(atividade.get("dataTermino")));
            row.put("Status", val(atividade.get("statusImplementacao")));
            row.put("Ações preventivas (descrever)", val(atividade.get("gatilho")));
            row.put("Monitoramento", ""); // Campo vazio para preenchimento

            row.put("Gatilho (descrever)", val(atividade.get("gatilho")));
            row.put("Ações de Contingência (descrever)", val(atividade.get("acoesContingencia")));
            row.put("Responsável", val(atividade.get("responsavelContingencia")));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 5. ATIVIDADES DE CONTROLE", rows);
        return result;
    }

    private static String extrairOpcaoTratamento(Map<String, Object> risco) {
        if (risco == null) return "";
        Map<String, Object> resposta = asMap(risco.get("respostaRisco"));
        return resposta == null ? "" : val(resposta.get("opcaoTratamento"));
    }

}
