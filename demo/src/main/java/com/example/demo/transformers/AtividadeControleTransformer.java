package com.example.demo.transformers;

import com.example.demo.contracts.AtividadesControleColumns;
import com.example.demo.contracts.SheetNames;
import com.example.demo.contracts.ValidationOptions;

import static com.example.demo.transformers.TransformerUtils.asMap;
import static com.example.demo.transformers.TransformerUtils.formatDateBr;
import static com.example.demo.transformers.TransformerUtils.getContent;
import static com.example.demo.transformers.TransformerUtils.val;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AtividadeControleTransformer {

    private AtividadeControleTransformer() {
    }

    // Aba ETAPA 5: Plano de tratamento e contingência
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> atividade : getContent(input)) {
            Map<String, Object> row = new LinkedHashMap<>();

            Map<String, Object> risco = asMap(atividade.get("risco"));
            row.put(AtividadesControleColumns.EVENTO_RISCO.key(), "");
            row.put(AtividadesControleColumns.OPCAO_TRATAMENTO.key(), extrairOpcaoTratamento(risco));
            row.put(AtividadesControleColumns.RESPONSAVEL_TRATAMENTO.key(), val(atividade.get("responsavelTratamento")));
            row.put(AtividadesControleColumns.DATA_INICIO.key(), formatDateBr(atividade.get("dataInicio")));
            row.put(AtividadesControleColumns.DATA_FIM.key(), formatDateBr(atividade.get("dataTermino")));
            row.put(AtividadesControleColumns.STATUS.key(), mapearStatusImplementacao(atividade.get("statusImplementacao")));
            row.put(AtividadesControleColumns.ACOES_PREVENTIVAS.key(), val(atividade.get("acoesPreventivas")));
            row.put(AtividadesControleColumns.MONITORAMENTO.key(), val(atividade.get("monitoramentoAcoesPreventivas")));
            row.put(AtividadesControleColumns.GATILHO.key(), val(atividade.get("gatilho")));
            row.put(AtividadesControleColumns.ACOES_CONTINGENCIA.key(), val(atividade.get("acoesContingencia")));
            row.put(AtividadesControleColumns.RESPONSAVEL_CONTINGENCIA.key(), val(atividade.get("responsavelContingencia")));

            rows.add(row);
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put(SheetNames.ETAPA_5.displayName(), rows);
        return result;
    }

    private static String mapearStatusImplementacao(Object statusObj) {
        if (statusObj == null) return ValidationOptions.STATUS_IMPLEMENTACAO[0];

        String status = String.valueOf(statusObj).toUpperCase();
        // Converte os valores do banco para o padrão da planilha
        return switch (status) {
            case "IMPLEMENTADO" -> ValidationOptions.STATUS_IMPLEMENTACAO[2];
            case "EM_IMPLEMENTACAO", "EMIMPLEMENTACAO" -> ValidationOptions.STATUS_IMPLEMENTACAO[1];
            default -> ValidationOptions.STATUS_IMPLEMENTACAO[0]; // Caso seja NAO_IMPLEMENTADO ou nulo
        };
    }

    private static String extrairOpcaoTratamento(Map<String, Object> risco) {
        if (risco == null) return "";

        Map<String, Object> resposta = asMap(risco.get("respostaRisco"));
        if (resposta == null) return "";

        String opcao = val(resposta.get("opcaoTratamento"));
        return ValidationOptions.normalizeOpcaoTratamento(opcao);
    }
}
