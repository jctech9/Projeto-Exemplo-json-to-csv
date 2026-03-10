package com.example.demo.transformers;

import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.getNestedString;
import static com.example.demo.transformers.TransformerUtils.val;
import java.util.*;

public class IdentificacaoEventosTransformer {

    // Aba ETAPA 2: Identificação e categorização de riscos
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String ultimoProcessoIdentificado = "";
        for (Map<String, Object> risco : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();


            String nomeProcesso = getNestedString(risco, "processo", "nome");

            // LÓGICA DE REPETIÇÃO:
            // Se o nome vier nulo ou vazio do backend, usamos o último que guardamos
            if (nomeProcesso == null || nomeProcesso.isEmpty()) {
                nomeProcesso = ultimoProcessoIdentificado;
            } else {
                // Se encontramos um nome novo, atualizamos a variável de memória
                ultimoProcessoIdentificado = nomeProcesso;
            }

            row.put("Processo", nomeProcesso);
            row.put("Fase", val(risco.get("faseProcesso")));
            row.put("Evento de Risco (indicar)", val(risco.get("nome")));
            row.put("Tipo de Risco", val(risco.get("tipoRisco")));
            row.put("Categoria", getNestedString(risco, "categoria", "nome"));
            row.put("Tipo de Risco de Integridade", getNestedString(risco, "tipoRiscoIntegridade", "nome"));
            row.put("Causas (descrever)", val(risco.get("causa")));
            row.put("Consequências (descrever)", val(risco.get("consequencias")));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("ETAPA 2. IDENTIFICAÇÃO DE EVENTOS", rows);
        return result;
    }

}
