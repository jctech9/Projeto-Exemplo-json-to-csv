package com.example.demo.application.export.input.payload;

import com.example.demo.domain.risco.RiscoAlignmentService;
import com.example.demo.domain.risco.RiscoValidationService;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadSheetBuilderTest {

    @Test
    void shouldUseExplicitTipoContractInSinglePayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipo", "avaliacoesRiscoControle");
        payload.put("content", List.of(validAvaliacaoItem(10)));

        Map<String, List<Map<String, Object>>> sheets = buildBuilder().buildSheetsFromPayload(payload);

        String etapa3Sheet = findSheetByPrefix(sheets, "ETAPA 3");
        assertNotNull(etapa3Sheet);
        assertEquals(1, sheets.get(etapa3Sheet).size());
    }

    @Test
    void shouldUseExplicitEtapaContractInSinglePayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("etapa", "ETAPA 5");
        payload.put("content", List.of(validAtividadeItem(20)));

        Map<String, List<Map<String, Object>>> sheets = buildBuilder().buildSheetsFromPayload(payload);

        String etapa5Sheet = findSheetByPrefix(sheets, "ETAPA 5");
        assertNotNull(etapa5Sheet);
        assertEquals(1, sheets.get(etapa5Sheet).size());
    }

    @Test
    void shouldFailWhenExplicitContractDoesNotMatchItemSchema() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipo", "respostasRisco");
        payload.put("content", List.of(
                validRespostaItem(1),
                Map.of("risco", Map.of("id", 2))
        ));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> buildBuilder().buildSheetsFromPayload(payload)
        );

        assertTrue(ex.getMessage().contains("schema esperado"));
    }

    @Test
    void shouldFailWhenSchemaInferenceIsNotPossibleWithoutContract() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", List.of(Map.of("risco", Map.of("id", 10))));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> buildBuilder().buildSheetsFromPayload(payload)
        );

        assertTrue(ex.getMessage().contains("nao foi possivel identificar"));
    }

    @Test
    void shouldInferTypeBySchemaWhenUnambiguous() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", List.of(
                validOcorrenciaItem(10, "2025-01-10", "Falha 1"),
                validOcorrenciaItem(11, "2025-02-10", "Falha 2")
        ));

        Map<String, List<Map<String, Object>>> sheets = buildBuilder().buildSheetsFromPayload(payload);

        String ocorrenciaSheet = sheets.keySet().stream()
                .filter(name -> name.toUpperCase().contains("OCORR"))
                .findFirst()
                .orElse(null);

        assertNotNull(ocorrenciaSheet);
        assertEquals(2, sheets.get(ocorrenciaSheet).size());
    }

    @Test
    void shouldFailWhenTipoAndEtapaContractsConflict() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipo", "riscos");
        payload.put("etapa", 4);
        payload.put("content", List.of(validRiscoItem(3)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> buildBuilder().buildSheetsFromPayload(payload)
        );

        assertTrue(ex.getMessage().contains("apontam para colecoes diferentes"));
    }

    private PayloadSheetBuilder buildBuilder() {
        RiscoAlignmentService alignmentService = new RiscoAlignmentService();
        RiscoValidationService validationService = new RiscoValidationService(alignmentService);
        return new PayloadSheetBuilder(alignmentService, validationService);
    }

    private String findSheetByPrefix(Map<String, List<Map<String, Object>>> sheets, String prefix) {
        return sheets.keySet().stream()
                .filter(name -> name.startsWith(prefix))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> validAvaliacaoItem(int riscoId) {
        return new LinkedHashMap<>(Map.of(
                "probabilidade", 5,
                "impacto", 8,
                "fac", 0.6,
                "controlesPreventivos", "Controle preventivo",
                "controlesAtenuacao", "Controle de atenuacao",
                "risco", Map.of("id", riscoId)
        ));
    }

    private Map<String, Object> validAtividadeItem(int riscoId) {
        return new LinkedHashMap<>(Map.of(
                "statusImplementacao", "IMPLEMENTADO",
                "responsavelTratamento", "Time",
                "acoesPreventivas", "Plano",
                "risco", Map.of("id", riscoId)
        ));
    }

    private Map<String, Object> validRespostaItem(int riscoId) {
        return new LinkedHashMap<>(Map.of(
                "opcaoTratamento", "MITIGAR",
                "justificativa", "Justificativa",
                "risco", Map.of("id", riscoId)
        ));
    }

    private Map<String, Object> validRiscoItem(int riscoId) {
        return new LinkedHashMap<>(Map.of(
                "id", riscoId,
                "faseProcesso", "Execucao",
                "nome", "Risco " + riscoId,
                "tipoRisco", "Operacional"
        ));
    }

    private Map<String, Object> validOcorrenciaItem(int riscoId, String data, String descricao) {
        return new LinkedHashMap<>(Map.of(
                "dataOcorrencia", data,
                "descricao", descricao,
                "responsavelSolucao", "Responsavel",
                "solucao", "Solucao",
                "resultados", "Resultado",
                "risco", Map.of("id", riscoId, "nome", "Risco " + riscoId)
        ));
    }
}
