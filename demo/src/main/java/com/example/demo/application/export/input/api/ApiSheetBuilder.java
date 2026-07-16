package com.example.demo.application.export.input.api;

import com.example.demo.transformers.AtividadeControleTransformer;
import com.example.demo.transformers.AvaliacaoRiscosTransformer;
import com.example.demo.transformers.RespostaRiscosTransformer;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
// Orquestra montagem de abas delegando responsabilidades por endpoint/dominio.
public class ApiSheetBuilder {

    private final ApiProcessSheetService processSheetService;
    private final ApiRiscoSheetService riscoSheetService;
    private final ApiRiscoCollectionSheetService riscoCollectionSheetService;
    private final ApiOcorrenciaSheetService ocorrenciaSheetService;

    public ApiSheetBuilder(
            ApiProcessSheetService processSheetService,
            ApiRiscoSheetService riscoSheetService,
            ApiRiscoCollectionSheetService riscoCollectionSheetService,
            ApiOcorrenciaSheetService ocorrenciaSheetService
    ) {
        this.processSheetService = processSheetService;
        this.riscoSheetService = riscoSheetService;
        this.riscoCollectionSheetService = riscoCollectionSheetService;
        this.ocorrenciaSheetService = ocorrenciaSheetService;
    }

    public Map<String, List<Map<String, Object>>> buildSheetsFromApi(int processId) {
        if (processId <= 0) {
            throw new IllegalArgumentException("Parametro id deve ser maior que zero.");
        }

        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();
        allSheets.putAll(processSheetService.buildProcessSheets(processId));

        ApiRiscoContext riscoContext = riscoSheetService.buildRiscoContext(processId);
        allSheets.putAll(riscoContext.sheets());

        allSheets.putAll(riscoCollectionSheetService.buildAlignedSheet(
                "/avaliacoesRiscoControle",
                processId,
                riscoContext.riscoIdsDoProcesso(),
                "avaliacoesRiscoControle",
                AvaliacaoRiscosTransformer::transform
        ));

        allSheets.putAll(riscoCollectionSheetService.buildAlignedSheet(
                "/respostasRisco",
                processId,
                riscoContext.riscoIdsDoProcesso(),
                "respostasRisco",
                RespostaRiscosTransformer::transform
        ));

        allSheets.putAll(riscoCollectionSheetService.buildAlignedSheet(
                "/atividadeControles",
                processId,
                riscoContext.riscoIdsDoProcesso(),
                "atividadeControles",
                AtividadeControleTransformer::transform
        ));

        allSheets.putAll(ocorrenciaSheetService.buildOcorrenciaSheets(
                riscoContext.riscoIdsDoProcesso(),
                riscoContext.riscoNomePorId()
        ));

        return allSheets;
    }
}
