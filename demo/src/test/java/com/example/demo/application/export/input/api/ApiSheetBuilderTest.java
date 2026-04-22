package com.example.demo.application.export.input.api;

import com.example.demo.domain.risco.RiscoAlignmentService;
import com.example.demo.domain.risco.RiscoValidationService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiSheetBuilderTest {

    @Test
    void shouldFailWhenProcessIdIsInvalid() {
        ApiHttpClient apiHttpClient = mock(ApiHttpClient.class);
        ApiSheetBuilder builder = createBuilder(apiHttpClient);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> builder.buildSheetsFromApi("http://localhost:8090", 0)
        );

        assertTrue(ex.getMessage().contains("maior que zero"));
    }

    @Test
    void shouldFailWhenProcessIdDoesNotExistOnSourceApi() {
        String baseUrl = "http://localhost:8090";
        ApiHttpClient apiHttpClient = mock(ApiHttpClient.class);
        ApiSheetBuilder builder = createBuilder(apiHttpClient);

        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/processos"))).thenReturn(payload(List.of(
                processo(2, "Processo 2")
        )));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> builder.buildSheetsFromApi(baseUrl, 1)
        );

        assertTrue(ex.getMessage().contains("nao foi encontrado"));
    }

    @Test
    void shouldFetchOcorrenciasInBatchWithoutNPlusOne() {
        String baseUrl = "http://localhost:8090";
        ApiHttpClient apiHttpClient = mock(ApiHttpClient.class);
        ApiSheetBuilder builder = createBuilder(apiHttpClient);

        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/processos"))).thenReturn(payload(List.of(
                processo(1, "Processo 1"),
                processo(2, "Processo 2")
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/riscos"))).thenReturn(payload(List.of(
                risco(10, 1, "Risco 10"),
                risco(99, 2, "Risco 99")
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/avaliacoesRiscoControle"))).thenReturn(payload(List.of(
                avaliacao(10, 1),
                avaliacao(99, 2)
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/respostasRisco"))).thenReturn(payload(List.of(
                resposta(10, 1),
                resposta(99, 2)
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/atividadeControles"))).thenReturn(payload(List.of(
                atividade(10, 1),
                atividade(99, 2)
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/ocorrenciasRisco"))).thenReturn(payload(List.of(
                ocorrencia(10, ""),
                ocorrencia(99, "Risco 99")
        )));

        Map<String, List<Map<String, Object>>> sheets = builder.buildSheetsFromApi(baseUrl, 1);

        String ocorrenciasSheet = sheets.keySet().stream()
                .filter(name -> name.toUpperCase().contains("OCORR"))
                .findFirst()
                .orElse(null);
        assertNotNull(ocorrenciasSheet);
        assertEquals(1, sheets.get(ocorrenciasSheet).size());
        assertEquals("Risco 10", sheets.get(ocorrenciasSheet).get(0).get("Evento de Risco"));

        verify(apiHttpClient, times(1)).fetchAllPages(baseUrl, "/ocorrenciasRisco");
        verify(apiHttpClient, never()).fetchAllPages(
                eq(baseUrl),
                argThat(endpoint -> endpoint != null && endpoint.startsWith("/ocorrenciasRisco/risco/"))
        );
    }

    @Test
    void shouldCaptureOcorrenciasWhenRiscoIdsComeAsString() {
        String baseUrl = "http://localhost:8090";
        ApiHttpClient apiHttpClient = mock(ApiHttpClient.class);
        ApiSheetBuilder builder = createBuilder(apiHttpClient);

        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/processos"))).thenReturn(payload(List.of(
                processo(1, "Processo 1")
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/riscos"))).thenReturn(payload(List.of(
                riscoComIdString("10", 1, "Risco 10")
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/avaliacoesRiscoControle"))).thenReturn(payload(List.of()));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/respostasRisco"))).thenReturn(payload(List.of()));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/atividadeControles"))).thenReturn(payload(List.of()));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/ocorrenciasRisco"))).thenReturn(payload(List.of(
                ocorrenciaComIdString("10", "")
        )));

        Map<String, List<Map<String, Object>>> sheets = builder.buildSheetsFromApi(baseUrl, 1);

        String ocorrenciasSheet = sheets.keySet().stream()
                .filter(name -> name.toUpperCase().contains("OCORR"))
                .findFirst()
                .orElse(null);
        assertNotNull(ocorrenciasSheet);
        assertEquals(1, sheets.get(ocorrenciasSheet).size());
        assertEquals("Risco 10", sheets.get(ocorrenciasSheet).get(0).get("Evento de Risco"));
    }

    @Test
    void shouldFallbackToPerRiskEndpointWhenBatchOcorrenciasHasNoRiscoReference() {
        String baseUrl = "http://localhost:8090";
        ApiHttpClient apiHttpClient = mock(ApiHttpClient.class);
        ApiSheetBuilder builder = createBuilder(apiHttpClient);

        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/processos"))).thenReturn(payload(List.of(
                processo(1, "Processo 1")
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/riscos"))).thenReturn(payload(List.of(
                risco(10, 1, "Risco 10")
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/avaliacoesRiscoControle"))).thenReturn(payload(List.of()));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/respostasRisco"))).thenReturn(payload(List.of()));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/atividadeControles"))).thenReturn(payload(List.of()));

        Map<String, Object> ocorrenciaSemRisco = new LinkedHashMap<>();
        ocorrenciaSemRisco.put("dataOcorrencia", "2026-01-01");
        ocorrenciaSemRisco.put("descricao", "Descricao sem risco no agregado");
        ocorrenciaSemRisco.put("responsavelSolucao", "Responsavel");
        ocorrenciaSemRisco.put("solucao", "Solucao");
        ocorrenciaSemRisco.put("resultados", "Resultados");
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/ocorrenciasRisco")))
                .thenReturn(payload(List.of(ocorrenciaSemRisco)));

        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/ocorrenciasRisco/risco/10")))
                .thenReturn(payload(List.of(ocorrenciaComIdString("10", ""))));

        Map<String, List<Map<String, Object>>> sheets = builder.buildSheetsFromApi(baseUrl, 1);

        String ocorrenciasSheet = sheets.keySet().stream()
                .filter(name -> name.toUpperCase().contains("OCORR"))
                .findFirst()
                .orElse(null);
        assertNotNull(ocorrenciasSheet);
        assertEquals(1, sheets.get(ocorrenciasSheet).size());
        assertEquals("Risco 10", sheets.get(ocorrenciasSheet).get(0).get("Evento de Risco"));

        verify(apiHttpClient, times(1)).fetchAllPages(baseUrl, "/ocorrenciasRisco");
        verify(apiHttpClient, times(1)).fetchAllPages(baseUrl, "/ocorrenciasRisco/risco/10");
    }

    @Test
    void shouldAttachDynamicCategoriaOptionsMetadataToEtapa2Rows() {
        String baseUrl = "http://localhost:8090";
        ApiHttpClient apiHttpClient = mock(ApiHttpClient.class);
        ApiSheetBuilder builder = createBuilder(apiHttpClient);

        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/processos"))).thenReturn(payload(List.of(
                processo(1, "Processo 1")
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/categoriasRisco"))).thenReturn(payload(List.of(
                categoria(1, "Categoria A"),
                categoria(2, "Categoria B")
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/riscos"))).thenReturn(payload(List.of(
                risco(10, 1, "Risco 10")
        )));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/avaliacoesRiscoControle"))).thenReturn(payload(List.of()));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/respostasRisco"))).thenReturn(payload(List.of()));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/atividadeControles"))).thenReturn(payload(List.of()));
        when(apiHttpClient.fetchAllPages(eq(baseUrl), eq("/ocorrenciasRisco"))).thenReturn(payload(List.of()));

        Map<String, List<Map<String, Object>>> sheets = builder.buildSheetsFromApi(baseUrl, 1);

        String etapa2Sheet = sheets.keySet().stream()
                .filter(name -> name.toUpperCase().contains("ETAPA 2") || name.toUpperCase().contains("EVENTOS"))
                .findFirst()
                .orElse(null);
        assertNotNull(etapa2Sheet);

        List<Map<String, Object>> etapa2Rows = sheets.get(etapa2Sheet);
        assertNotNull(etapa2Rows);
        assertTrue(!etapa2Rows.isEmpty());

        Map<String, Object> metadataRow = etapa2Rows.get(0);
        assertEquals("etapa2_options", metadataRow.get("__meta_row_type"));
        assertEquals(List.of("Categoria A", "Categoria B"), metadataRow.get("__meta_categoria_options"));
    }

    private ApiSheetBuilder createBuilder(ApiHttpClient apiHttpClient) {
        RiscoAlignmentService riscoAlignmentService = new RiscoAlignmentService();
        RiscoValidationService riscoValidationService = new RiscoValidationService(riscoAlignmentService);

        ApiEndpointDataService endpointDataService = new ApiEndpointDataService(apiHttpClient);
        ApiPayloadFilterService payloadFilterService = new ApiPayloadFilterService();

        return new ApiSheetBuilder(
                new ApiProcessSheetService(endpointDataService, payloadFilterService),
                new ApiRiscoSheetService(endpointDataService, payloadFilterService),
                new ApiRiscoCollectionSheetService(
                        endpointDataService,
                        payloadFilterService,
                        riscoAlignmentService,
                        riscoValidationService
                ),
                new ApiOcorrenciaSheetService(endpointDataService, payloadFilterService, riscoAlignmentService)
        );
    }

    private Map<String, Object> payload(List<Map<String, Object>> content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", new ArrayList<>(content));
        payload.put("totalPages", 1);
        payload.put("last", true);
        return payload;
    }

    private Map<String, Object> processo(int id, String nome) {
        Map<String, Object> processo = new LinkedHashMap<>();
        processo.put("id", id);
        processo.put("nome", nome);
        processo.put("objetivosGerais", "Objetivos " + id);
        processo.put("unidadeOrganizacional", new LinkedHashMap<>(Map.of("sigla", "U" + id, "nome", "Unidade " + id)));
        processo.put("responsavel", new LinkedHashMap<>(Map.of("nome", "Responsavel " + id)));
        return processo;
    }

        private Map<String, Object> categoria(int id, String nome) {
                Map<String, Object> categoria = new LinkedHashMap<>();
                categoria.put("id", id);
                categoria.put("nome", nome);
                return categoria;
        }

    private Map<String, Object> risco(int riscoId, int processoId, String nomeRisco) {
        Map<String, Object> risco = new LinkedHashMap<>();
        risco.put("id", riscoId);
        risco.put("nome", nomeRisco);
        risco.put("faseProcesso", "Execucao");
        risco.put("tipoRisco", "Ameaça");
        risco.put("categoria", new LinkedHashMap<>(Map.of("nome", "Operacionais")));
        risco.put("processo", new LinkedHashMap<>(Map.of("id", processoId)));
        return risco;
    }

    private Map<String, Object> riscoComIdString(String riscoId, int processoId, String nomeRisco) {
        Map<String, Object> risco = new LinkedHashMap<>();
        risco.put("id", riscoId);
        risco.put("nome", nomeRisco);
        risco.put("faseProcesso", "Execucao");
        risco.put("tipoRisco", "Ameaça");
        risco.put("categoria", new LinkedHashMap<>(Map.of("nome", "Operacionais")));
        risco.put("processo", new LinkedHashMap<>(Map.of("id", processoId)));
        return risco;
    }

    private Map<String, Object> avaliacao(int riscoId, int processoId) {
        Map<String, Object> avaliacao = new LinkedHashMap<>();
        avaliacao.put("probabilidade", 5);
        avaliacao.put("impacto", 5);
        avaliacao.put("fac", 0.4);
        avaliacao.put("risco", new LinkedHashMap<>(Map.of(
                "id", riscoId,
                "processo", new LinkedHashMap<>(Map.of("id", processoId))
        )));
        return avaliacao;
    }

    private Map<String, Object> resposta(int riscoId, int processoId) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("opcaoTratamento", "Mitigar");
        resposta.put("justificativa", "Justificativa");
        resposta.put("risco", new LinkedHashMap<>(Map.of(
                "id", riscoId,
                "faseProcesso", "Execucao",
                "processo", new LinkedHashMap<>(Map.of("id", processoId))
        )));
        return resposta;
    }

    private Map<String, Object> atividade(int riscoId, int processoId) {
        Map<String, Object> atividade = new LinkedHashMap<>();
        atividade.put("statusImplementacao", "IMPLEMENTADO");
        atividade.put("responsavelTratamento", "Time");
        atividade.put("acoesPreventivas", "Plano");
        atividade.put("monitoramentoAcoesPreventivas", "Monitoramento");
        atividade.put("gatilho", "Gatilho");
        atividade.put("acoesContingencia", "Contingencia");
        atividade.put("responsavelContingencia", "Responsavel");
        atividade.put("risco", new LinkedHashMap<>(Map.of(
                "id", riscoId,
                "processo", new LinkedHashMap<>(Map.of("id", processoId)),
                "respostaRisco", new LinkedHashMap<>(Map.of("opcaoTratamento", "Mitigar"))
        )));
        return atividade;
    }

    private Map<String, Object> ocorrencia(int riscoId, String nomeRisco) {
        Map<String, Object> ocorrencia = new LinkedHashMap<>();
        ocorrencia.put("dataOcorrencia", "2026-01-01");
        ocorrencia.put("descricao", "Descricao");
        ocorrencia.put("responsavelSolucao", "Responsavel");
        ocorrencia.put("solucao", "Solucao");
        ocorrencia.put("resultados", "Resultados");
        ocorrencia.put("risco", new LinkedHashMap<>(Map.of(
                "id", riscoId,
                "nome", nomeRisco
        )));
        return ocorrencia;
    }

    private Map<String, Object> ocorrenciaComIdString(String riscoId, String nomeRisco) {
        Map<String, Object> ocorrencia = new LinkedHashMap<>();
        ocorrencia.put("dataOcorrencia", "2026-01-01");
        ocorrencia.put("descricao", "Descricao");
        ocorrencia.put("responsavelSolucao", "Responsavel");
        ocorrencia.put("solucao", "Solucao");
        ocorrencia.put("resultados", "Resultados");
        ocorrencia.put("risco", new LinkedHashMap<>(Map.of(
                "id", riscoId,
                "nome", nomeRisco
        )));
        return ocorrencia;
    }
}
