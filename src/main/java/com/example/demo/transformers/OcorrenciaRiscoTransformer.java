package com.example.demo.transformers;

import com.example.demo.contracts.SheetNames;

import static com.example.demo.transformers.TransformerUtils.asMap;
import static com.example.demo.transformers.TransformerUtils.formatDateBr;
import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.val;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OcorrenciaRiscoTransformer {

    private OcorrenciaRiscoTransformer() {
    }

    // Aba OCORRÊNCIAS DE RISCO: evento, data, descrição, responsável, solução, resultados
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> ocorrencia : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();

            // Buscar o evento de risco relacionado
            row.put("Evento de Risco", extrairEventoRisco(ocorrencia));
            row.put("Data da Ocorrência", formatDateBr(ocorrencia.get("dataOcorrencia")));
            row.put("Descrição da Ocorrência", val(ocorrencia.get("descricao")));
            row.put("Responsável pela Solução", val(ocorrencia.get("responsavelSolucao")));
            row.put("Solução", val(ocorrencia.get("solucao")));
            row.put("Resultados", val(ocorrencia.get("resultados")));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put(SheetNames.OCORRENCIAS_RISCO.displayName(), rows);
        return result;
    }

    private static String extrairEventoRisco(Map<?, ?> ocorrencia) {
        // Extrair nome do risco associado à ocorrência
        Map<String, Object> risco = asMap(ocorrencia.get("risco"));
        if (risco != null) {
            String nome = val(risco.get("nome"));
            if (!nome.isEmpty()) return nome;

            String eventoRisco = val(risco.get("eventoRisco"));
            if (!eventoRisco.isEmpty()) return eventoRisco;

            String descricaoRisco = val(risco.get("descricao"));
            if (!descricaoRisco.isEmpty()) return descricaoRisco;
        }

        // Não usar descrição da ocorrência na coluna de evento de risco.
        return "";
    }
}
