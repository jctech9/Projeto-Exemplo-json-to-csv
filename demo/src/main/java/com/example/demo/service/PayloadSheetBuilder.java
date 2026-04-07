package com.example.demo.service;

import com.example.demo.transformers.AtividadeControleTransformer;
import com.example.demo.transformers.AvaliacaoRiscosTransformer;
import com.example.demo.transformers.DadosProcessoTransformer;
import com.example.demo.transformers.IdentificacaoEventosTransformer;
import com.example.demo.transformers.OcorrenciaRiscoTransformer;
import com.example.demo.transformers.RefResolver;
import com.example.demo.transformers.RespostaRiscosTransformer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
// Converte payloads (simples ou combinados) em colecoes de abas.
public class PayloadSheetBuilder {

    private final RiscoAlignmentService riscoAlignmentService;
    private final RiscoValidationService riscoValidationService;

    public PayloadSheetBuilder(
            RiscoAlignmentService riscoAlignmentService,
            RiscoValidationService riscoValidationService
    ) {
        this.riscoAlignmentService = riscoAlignmentService;
        this.riscoValidationService = riscoValidationService;
    }

    public Map<String, List<Map<String, Object>>> buildSheetsFromPayload(Map<String, Object> payload) {
        RefResolver.resolve(payload);

        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();

        if (isCombinedPayload(payload)) {
            transformCombinedPayload(payload, allSheets);
        } else if (payload.containsKey("content")) {
            Object content = payload.get("content");
            if (content instanceof List<?> contentList && !contentList.isEmpty()) {
                Object firstItem = contentList.get(0);
                if (firstItem instanceof Map<?, ?> item) {

                    if (item.containsKey("dataOcorrencia") && item.containsKey("descricao")) {
                        allSheets.putAll(OcorrenciaRiscoTransformer.transform(payload));
                    } else if (item.containsKey("statusImplementacao") && item.containsKey("risco")) {
                        allSheets.putAll(AtividadeControleTransformer.transform(payload));
                    } else if (item.containsKey("probabilidade") && item.containsKey("risco")) {
                        allSheets.putAll(AvaliacaoRiscosTransformer.transform(payload));
                    } else if (item.containsKey("risco")) {
                        allSheets.putAll(RespostaRiscosTransformer.transform(payload));
                    } else if (item.containsKey("faseProcesso")) {
                        allSheets.putAll(IdentificacaoEventosTransformer.transform(payload));
                    } else {
                        allSheets.putAll(DadosProcessoTransformer.transform(payload));
                    }
                }
            }
        }

        return allSheets;
    }

    private boolean isCombinedPayload(Map<String, Object> payload) {
        return payload.containsKey("processos")
                || payload.containsKey("riscos")
                || payload.containsKey("avaliacoesRiscoControle")
                || payload.containsKey("respostasRisco")
                || payload.containsKey("atividadeControles")
                || payload.containsKey("ocorrenciasRisco");
    }

    private void transformCombinedPayload(
            Map<String, Object> payload,
            Map<String, List<Map<String, Object>>> allSheets
    ) {
        Map<String, Object> processosData = getPayloadSection(payload, "processos");
        Map<String, Object> riscosData = getPayloadSection(payload, "riscos");
        Map<String, Object> avaliacoesData = getPayloadSection(payload, "avaliacoesRiscoControle");
        Map<String, Object> respostasData = getPayloadSection(payload, "respostasRisco");
        Map<String, Object> atividadesData = getPayloadSection(payload, "atividadeControles");
        Map<String, Object> ocorrenciasData = getPayloadSection(payload, "ocorrenciasRisco");

        if (processosData != null) {
            allSheets.putAll(DadosProcessoTransformer.transform(processosData));
        }

        List<Map<String, Object>> riscosContent = getContentOrEmpty(riscosData);
        List<Map<String, Object>> avaliacoesContent = getContentOrEmpty(avaliacoesData);
        List<Map<String, Object>> respostasContent = getContentOrEmpty(respostasData);
        List<Map<String, Object>> atividadesContent = getContentOrEmpty(atividadesData);
        List<Map<String, Object>> ocorrenciasContent = getContentOrEmpty(ocorrenciasData);

        // Define a ordem canonica das etapas para manter alinhamento por risco.id.
        Set<Integer> canonicalRiscoIds = riscoValidationService.buildCanonicalRiscoIds(
                riscosContent,
                avaliacoesContent,
                respostasContent,
                atividadesContent,
                ocorrenciasContent
        );

        Map<Integer, String> riscoNomePorId = riscoAlignmentService.buildRiscoNomePorId(riscosContent);

        int colecoesComRisco = riscoValidationService.countNonEmptyCollections(
                riscosContent,
                avaliacoesContent,
                respostasContent,
                atividadesContent
        );
        if (colecoesComRisco > 1 && canonicalRiscoIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: nao foi possivel montar uma lista canonica de risco.id para alinhar as etapas."
            );
        }

        if (riscosData != null) {
            allSheets.putAll(IdentificacaoEventosTransformer.transform(riscosData));
        }

        if (avaliacoesData != null) {
            riscoValidationService.validateStrictRiscoCollection("avaliacoesRiscoControle", avaliacoesContent, canonicalRiscoIds);
            avaliacoesData.put("content", riscoAlignmentService.alignByRiscoIds(avaliacoesContent, canonicalRiscoIds));
            allSheets.putAll(AvaliacaoRiscosTransformer.transform(avaliacoesData));
        }

        if (respostasData != null) {
            riscoValidationService.validateStrictRiscoCollection("respostasRisco", respostasContent, canonicalRiscoIds);
            respostasData.put("content", riscoAlignmentService.alignByRiscoIds(respostasContent, canonicalRiscoIds));
            allSheets.putAll(RespostaRiscosTransformer.transform(respostasData));
        }

        if (atividadesData != null) {
            riscoValidationService.validateStrictRiscoCollection("atividadeControles", atividadesContent, canonicalRiscoIds);
            atividadesData.put("content", riscoAlignmentService.alignByRiscoIds(atividadesContent, canonicalRiscoIds));
            allSheets.putAll(AtividadeControleTransformer.transform(atividadesData));
        }

        if (ocorrenciasData != null) {
            List<Map<String, Object>> ocorrenciasAlinhadas = riscoAlignmentService.alignByRiscoIds(ocorrenciasContent, canonicalRiscoIds);
            riscoAlignmentService.applyCanonicalRiscoNome(ocorrenciasAlinhadas, riscoNomePorId);
            ocorrenciasData.put("content", ocorrenciasAlinhadas);
            allSheets.putAll(OcorrenciaRiscoTransformer.transform(ocorrenciasData));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPayloadSection(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new IllegalArgumentException(
                "Inconsistencia no payload: a colecao '" + key + "' deve ser um objeto com campo 'content'."
        );
    }

    private List<Map<String, Object>> getContentOrEmpty(Map<String, Object> data) {
        if (data == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> content = getList(data);
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(content);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> data) {
        if (data.containsKey("content")) {
            return (List<Map<String, Object>>) data.get("content");
        }
        return null;
    }
}
