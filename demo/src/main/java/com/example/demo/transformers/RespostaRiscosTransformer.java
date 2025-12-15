package com.example.demo.transformers;

import static com.example.demo.transformers.TransformerUtils.asMap;
import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.getNestedString;
import static com.example.demo.transformers.TransformerUtils.val;
import java.util.*;

public class RespostaRiscosTransformer {

    // Aba ETAPA 4: Respostas aos riscos (processo, fase, evento, tratamento, justificativa)
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> respostaRisco : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();
            Map<String, Object> risco = asMap(respostaRisco.get("risco"));

            if (risco != null) {
                row.put("Processo", getNestedString(risco, "processo", "nome"));
                row.put("Fase", val(risco.get("faseProcesso")));
                row.put("Evento de Risco", val(risco.get("nome")));
            } else {
                row.put("Processo", "");
                row.put("Fase", "");
                row.put("Evento de Risco", "");
            }

            row.put("Opção de Tratamento", val(respostaRisco.get("opcaoTratamento")));
            row.put("Justificativa da escolha da opção de tratamento", val(respostaRisco.get("justificativa")));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 4. RESPOSTA AOS RISCOS", rows);
        return result;
    }

}
