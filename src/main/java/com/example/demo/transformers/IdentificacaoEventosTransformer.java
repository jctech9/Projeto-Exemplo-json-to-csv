package com.example.demo.transformers;

import com.example.demo.contracts.SheetNames;

import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.getNestedString;
import static com.example.demo.transformers.TransformerUtils.val;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IdentificacaoEventosTransformer {

    private IdentificacaoEventosTransformer() {
    }

    // Aba ETAPA 2: Identificação e categorização de riscos
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> risco : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();

            // A coluna Processo é preenchida por fórmula no service.
            row.put("Processo", "");
            row.put("Fase", val(risco.get("faseProcesso")));
            row.put("Evento de Risco (indicar)", val(risco.get("nome")));
            row.put("Tipo de Risco", val(risco.get("tipoRisco")));
            String categoria = getNestedString(risco, "categoria", "nome");
            String tipoRiscoIntegridade = getNestedString(risco, "tipoRiscoIntegridade", "nome");

            row.put("Categoria", categoria);
            row.put("Tipo de Risco de Integridade", isCategoriaIntegridade(categoria) ? tipoRiscoIntegridade : "");
            row.put("Causas (descrever)", val(risco.get("causa")));
            row.put("Consequências (descrever)", val(risco.get("consequencias")));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put(SheetNames.ETAPA_2.displayName(), rows);
        return result;
    }

    private static boolean isCategoriaIntegridade(String categoria) {
        if (categoria == null) {
            return false;
        }

        String normalized = Normalizer.normalize(categoria, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim();

        return "Integridade".equalsIgnoreCase(normalized);
    }
}
