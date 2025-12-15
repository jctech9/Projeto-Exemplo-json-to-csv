package com.example.demo.transformers;

import static com.example.demo.transformers.TransformerUtils.asMap;
import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.getNestedString;
import static com.example.demo.transformers.TransformerUtils.val;
import java.util.*;

public class OcorrenciaRiscoTransformer {

    // Aba OCORRÊNCIAS DE RISCO: evento, data, descrição, responsável, solução, resultados
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> ocorrencia : getContent(input)) {
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

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("OCORRÊNCIAS DE RISCO", rows);
        return result;
    }

    private static String extrairEventoRisco(Map<?, ?> ocorrencia) {
        // Tentar extrair de diferentes campos possíveis
        Map<String, Object> eventoRisco = asMap(ocorrencia.get("eventoRisco"));
        String evento = eventoRisco == null ? "" : val(eventoRisco.get("evento"));
        if (!evento.isEmpty()) return evento;

        Map<String, Object> risco = asMap(ocorrencia.get("risco"));
        if (risco != null) {
            String ev = val(risco.get("evento"));
            if (!ev.isEmpty()) return ev;
            String nome = val(risco.get("nome"));
            if (!nome.isEmpty()) return nome;
        }

        // fallback final: usa a própria descrição da ocorrência
        return val(ocorrencia.get("descricao"));
    }

}
