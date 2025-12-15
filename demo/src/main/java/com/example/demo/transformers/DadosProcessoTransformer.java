package com.example.demo.transformers;

import static com.example.demo.transformers.TransformerUtils.asMap;
import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.getNestedString;
import static com.example.demo.transformers.TransformerUtils.val;
import java.util.*;

public class DadosProcessoTransformer {

    // Aba ETAPA 1: Processo, Objetivos, Unidade, Responsável
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> processo : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Processo", val(processo.get("nome")));
            row.put("Objetivos do Processo (Geral e específicos)", val(processo.get("objetivosGerais")));

            Map<String, Object> unidade = asMap(processo.get("unidadeOrganizacional"));
            String sigla = unidade == null ? "" : val(unidade.get("sigla"));
            String nome = unidade == null ? "" : val(unidade.get("nome"));
            row.put("Unidade", sigla.isEmpty() ? nome : sigla);

            row.put("Responsável", getNestedString(processo, "responsavel", "nome"));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 1. DADOS DO PROCESSO", rows);
        return result;
    }

}
