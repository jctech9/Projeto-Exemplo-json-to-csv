package com.example.demo.application.export.input.payload;

import com.example.demo.domain.risco.RiscoAlignmentService;
import com.example.demo.domain.risco.RiscoValidationService;

import com.example.demo.transformers.AtividadeControleTransformer;
import com.example.demo.transformers.AvaliacaoRiscosTransformer;
import com.example.demo.transformers.DadosProcessoTransformer;
import com.example.demo.transformers.IdentificacaoEventosTransformer;
import com.example.demo.transformers.OcorrenciaRiscoTransformer;
import com.example.demo.transformers.RefResolver;
import com.example.demo.transformers.RespostaRiscosTransformer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
// Converte payloads (simples ou combinados) em colecoes de abas.
public class PayloadSheetBuilder {

    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_TIPO = "tipo";
    private static final String FIELD_ETAPA = "etapa";
    private static final Pattern ETAPA_NUMBER_PATTERN = Pattern.compile("(\\d+)");

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

        // Payload combinado usa secoes nomeadas (processos, riscos, etc.).
        if (isCombinedPayload(payload)) {
            transformCombinedPayload(payload, allSheets);
            return allSheets;
        }

        // Payload simples exige contrato (tipo/etapa) ou inferencia sem ambiguidade.
        allSheets.putAll(transformSinglePayload(payload));

        return allSheets;
    }

    private Map<String, List<Map<String, Object>>> transformSinglePayload(Map<String, Object> payload) {
        List<Map<String, Object>> content = getRequiredContentList(payload);
        SinglePayloadType payloadType = resolveSinglePayloadType(payload, content);
        validateContentAgainstSchema(payloadType, content);
        return payloadType.transform(payload);
    }

    private SinglePayloadType resolveSinglePayloadType(Map<String, Object> payload, List<Map<String, Object>> content) {
        // Prioriza contrato explicito para evitar roteamento por heuristica fraca.
        SinglePayloadType explicitType = resolvePayloadTypeFromContract(payload);
        if (explicitType != null) {
            return explicitType;
        }

        if (content.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: sem itens em 'content' nao e possivel inferir a colecao. Informe 'tipo' ou 'etapa'."
            );
        }

        List<SinglePayloadType> compatibleTypes = Arrays.stream(SinglePayloadType.values())
                .filter(type -> type.matchesSchema(content))
                .toList();

        // Sem contrato, so aceita inferencia quando o schema aponta para um unico tipo.
        if (compatibleTypes.size() == 1) {
            return compatibleTypes.get(0);
        }

        if (compatibleTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: nao foi possivel identificar a colecao por schema. "
                            + "Informe o contrato explicito em 'tipo' ou 'etapa'."
            );
        }

        String candidates = compatibleTypes.stream()
                .map(SinglePayloadType::contractName)
                .collect(Collectors.joining(", "));

        throw new IllegalArgumentException(
                "Inconsistencia no payload: schema ambiguo para o payload simples. "
                        + "Candidatos: [" + candidates + "]. Informe 'tipo' ou 'etapa'."
        );
    }

    private SinglePayloadType resolvePayloadTypeFromContract(Map<String, Object> payload) {
        SinglePayloadType byTipo = resolvePayloadTypeByTipo(payload.get(FIELD_TIPO));
        SinglePayloadType byEtapa = resolvePayloadTypeByEtapa(payload.get(FIELD_ETAPA));

        if (byTipo != null && byEtapa != null && byTipo != byEtapa) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: os campos 'tipo' e 'etapa' apontam para colecoes diferentes."
            );
        }

        return byTipo != null ? byTipo : byEtapa;
    }

    private SinglePayloadType resolvePayloadTypeByTipo(Object tipoValue) {
        if (tipoValue == null) {
            return null;
        }

        String normalizedTipo = normalizeContractValue(String.valueOf(tipoValue));
        if (normalizedTipo.isBlank()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: campo 'tipo' vazio. Valores aceitos: " + supportedTipoContracts() + "."
            );
        }

        return Arrays.stream(SinglePayloadType.values())
                .filter(type -> type.matchesTipoAlias(normalizedTipo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inconsistencia no payload: valor de 'tipo' invalido: '" + tipoValue
                                + "'. Valores aceitos: " + supportedTipoContracts() + "."
                ));
    }

    private SinglePayloadType resolvePayloadTypeByEtapa(Object etapaValue) {
        if (etapaValue == null) {
            return null;
        }

        int etapaNumber = parseEtapaNumber(etapaValue);
        return Arrays.stream(SinglePayloadType.values())
                .filter(type -> type.matchesEtapa(etapaNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inconsistencia no payload: valor de 'etapa' invalido: '" + etapaValue
                                + "'. Valores aceitos: " + supportedEtapaContracts() + "."
                ));
    }

    private int parseEtapaNumber(Object etapaValue) {
        if (etapaValue instanceof Number n) {
            return n.intValue();
        }

        String raw = String.valueOf(etapaValue).trim();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: campo 'etapa' vazio. Valores aceitos: " + supportedEtapaContracts() + "."
            );
        }

        Matcher matcher = ETAPA_NUMBER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: valor de 'etapa' invalido: '" + etapaValue
                            + "'. Valores aceitos: " + supportedEtapaContracts() + "."
            );
        }

        return Integer.parseInt(matcher.group(1));
    }

    private void validateContentAgainstSchema(SinglePayloadType payloadType, List<Map<String, Object>> content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        // Valida todos os itens para bloquear payload heterogeneo/incompleto.
        List<Integer> invalidPositions = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            if (!payloadType.matchesItemSchema(content.get(i))) {
                invalidPositions.add(i + 1);
            }
        }

        if (!invalidPositions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: o contrato '" + payloadType.contractName()
                            + "' nao atende ao schema esperado nas posicoes " + invalidPositions + ". "
                            + "Campos esperados: " + payloadType.describeSchema() + "."
            );
        }
    }

    private String supportedTipoContracts() {
        return Arrays.stream(SinglePayloadType.values())
                .map(SinglePayloadType::contractName)
                .collect(Collectors.joining(", "));
    }

    private String supportedEtapaContracts() {
        return Arrays.stream(SinglePayloadType.values())
                .filter(SinglePayloadType::hasEtapa)
                .map(type -> String.valueOf(type.getEtapa()))
                .collect(Collectors.joining(", "));
    }

    private String normalizeContractValue(String rawValue) {
        return rawValue
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getRequiredContentList(Map<String, Object> payload) {
        // Garante o shape minimo antes de resolver tipo e aplicar transformer.
        if (!payload.containsKey(FIELD_CONTENT)) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: payload simples deve conter o campo 'content'."
            );
        }

        Object contentObj = payload.get(FIELD_CONTENT);
        if (!(contentObj instanceof List<?> list)) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: campo 'content' deve ser uma lista."
            );
        }

        List<Map<String, Object>> content = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map<?, ?> itemMap)) {
                throw new IllegalArgumentException(
                        "Inconsistencia no payload: item da lista 'content' na posicao " + (i + 1)
                                + " deve ser objeto."
                );
            }
            content.add((Map<String, Object>) itemMap);
        }
        return content;
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
        if (data.containsKey(FIELD_CONTENT)) {
            return (List<Map<String, Object>>) data.get(FIELD_CONTENT);
        }
        return null;
    }

    private enum SinglePayloadType {
        // Define contrato aceito por colecao e schema minimo por item.
        DADOS_PROCESSO(
                1,
                List.of("processos", "processo", "dados-processo", "dadosProcesso", "etapa1"),
                Set.of(),
                Set.of("objetivosGerais", "unidadeOrganizacional", "responsavel")
        ),
        IDENTIFICACAO_EVENTOS(
                2,
                List.of("riscos", "risco", "identificacao-eventos", "identificacaoEventos", "etapa2"),
                Set.of("faseProcesso"),
                Set.of()
        ),
        AVALIACAO_RISCOS(
                3,
                List.of("avaliacoesRiscoControle", "avaliacao-riscos", "avaliacaoRiscos", "etapa3"),
                Set.of("probabilidade", "impacto", "risco"),
                Set.of()
        ),
        RESPOSTA_RISCOS(
                4,
                List.of("respostasRisco", "resposta-riscos", "respostaRiscos", "etapa4"),
                Set.of("opcaoTratamento", "risco"),
                Set.of()
        ),
        ATIVIDADE_CONTROLES(
                5,
                List.of("atividadeControles", "atividades-controle", "atividadeControle", "etapa5"),
                Set.of("statusImplementacao", "risco"),
                Set.of()
        ),
        OCORRENCIAS_RISCO(
                null,
                List.of("ocorrenciasRisco", "ocorrencia-risco", "ocorrencias", "ocorrencia"),
                Set.of("dataOcorrencia", "descricao", "risco"),
                Set.of()
        );

        private final Integer etapa;
        private final List<String> tipoAliases;
        private final Set<String> requiredAllKeys;
        private final Set<String> requiredAnyKeys;

        SinglePayloadType(
                Integer etapa,
                List<String> tipoAliases,
                Set<String> requiredAllKeys,
                Set<String> requiredAnyKeys
        ) {
            this.etapa = etapa;
            this.tipoAliases = tipoAliases;
            this.requiredAllKeys = requiredAllKeys;
            this.requiredAnyKeys = requiredAnyKeys;
        }

        Integer getEtapa() {
            return etapa;
        }

        boolean hasEtapa() {
            return etapa != null;
        }

        boolean matchesEtapa(int etapa) {
            return this.etapa != null && this.etapa == etapa;
        }

        boolean matchesTipoAlias(String normalizedTipo) {
            return tipoAliases.stream()
                    .map(alias -> alias.toLowerCase().replaceAll("[^a-z0-9]", ""))
                    .anyMatch(alias -> alias.equals(normalizedTipo));
        }

        boolean matchesSchema(List<Map<String, Object>> content) {
            if (content == null || content.isEmpty()) {
                return false;
            }
            for (Map<String, Object> item : content) {
                if (!matchesItemSchema(item)) {
                    return false;
                }
            }
            return true;
        }

        boolean matchesItemSchema(Map<String, Object> item) {
            if (item == null) {
                return false;
            }

            for (String requiredKey : requiredAllKeys) {
                if (!item.containsKey(requiredKey)) {
                    return false;
                }
            }

            if (!requiredAnyKeys.isEmpty()) {
                for (String anyKey : requiredAnyKeys) {
                    if (item.containsKey(anyKey)) {
                        return true;
                    }
                }
                return false;
            }

            return true;
        }

        String describeSchema() {
            if (requiredAllKeys.isEmpty() && requiredAnyKeys.isEmpty()) {
                return "nenhum campo obrigatorio";
            }
            if (requiredAnyKeys.isEmpty()) {
                return "todos: " + requiredAllKeys;
            }
            if (requiredAllKeys.isEmpty()) {
                return "ao menos um de: " + requiredAnyKeys;
            }
            return "todos: " + requiredAllKeys + "; e ao menos um de: " + requiredAnyKeys;
        }

        String contractName() {
            return tipoAliases.get(0);
        }

        Map<String, List<Map<String, Object>>> transform(Map<String, Object> payload) {
            return switch (this) {
                case DADOS_PROCESSO -> DadosProcessoTransformer.transform(payload);
                case IDENTIFICACAO_EVENTOS -> IdentificacaoEventosTransformer.transform(payload);
                case AVALIACAO_RISCOS -> AvaliacaoRiscosTransformer.transform(payload);
                case RESPOSTA_RISCOS -> RespostaRiscosTransformer.transform(payload);
                case ATIVIDADE_CONTROLES -> AtividadeControleTransformer.transform(payload);
                case OCORRENCIAS_RISCO -> OcorrenciaRiscoTransformer.transform(payload);
            };
        }
    }
}
