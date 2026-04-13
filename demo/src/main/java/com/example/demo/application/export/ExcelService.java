package com.example.demo.application.export;

import com.example.demo.contracts.SheetNames;
import com.example.demo.infrastructure.excel.sheet.AtividadesControleService;
import com.example.demo.infrastructure.excel.sheet.AvaliacaoRiscosService;
import com.example.demo.infrastructure.excel.sheet.DadosProcessoService;
import com.example.demo.infrastructure.excel.sheet.IdentificacaoEventosService;
import com.example.demo.infrastructure.excel.sheet.OcorrenciaRiscoService;
import com.example.demo.infrastructure.excel.sheet.RespostaRiscosService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelService {

    private static final Pattern ETAPA_PATTERN = Pattern.compile("\\bETAPA\\s+(\\d+)\\b");

    private final DadosProcessoService dadosProcessoService;
    private final IdentificacaoEventosService identificacaoEventosService;
    private final AvaliacaoRiscosService avaliacaoRiscosService;
    private final RespostaRiscosService respostaRiscosService;
    private final AtividadesControleService atividadesControleService;
    private final OcorrenciaRiscoService ocorrenciaRiscoService;
    private final Map<SheetType, SheetGenerator> sheetGenerators;

    public ExcelService(
            DadosProcessoService dadosProcessoService,
            IdentificacaoEventosService identificacaoEventosService,
            AvaliacaoRiscosService avaliacaoRiscosService,
            RespostaRiscosService respostaRiscosService,
            AtividadesControleService atividadesControleService,
            OcorrenciaRiscoService ocorrenciaRiscoService
    ) {
        this.dadosProcessoService = dadosProcessoService;
        this.identificacaoEventosService = identificacaoEventosService;
        this.avaliacaoRiscosService = avaliacaoRiscosService;
        this.respostaRiscosService = respostaRiscosService;
        this.atividadesControleService = atividadesControleService;
        this.ocorrenciaRiscoService = ocorrenciaRiscoService;

        this.sheetGenerators = new EnumMap<>(SheetType.class);
        this.sheetGenerators.put(SheetType.ETAPA_1, dadosProcessoService::generateSheet);
        this.sheetGenerators.put(SheetType.ETAPA_2, identificacaoEventosService::generateSheet);
        this.sheetGenerators.put(SheetType.ETAPA_3, avaliacaoRiscosService::generateSheet);
        this.sheetGenerators.put(SheetType.ETAPA_4, respostaRiscosService::generateSheet);
        this.sheetGenerators.put(SheetType.ETAPA_5, atividadesControleService::generateSheet);
        this.sheetGenerators.put(SheetType.OCORRENCIA_RISCO, ocorrenciaRiscoService::generateSheet);
    }

    public byte[] generateXlsx(Map<String, List<Map<String, Object>>> etapas) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (Map.Entry<String, List<Map<String, Object>>> entry : orderByDependencies(etapas)) {

                String sheetName = entry.getKey();
                List<Map<String, Object>> rows = entry.getValue();
                SheetType sheetType = classifySheetType(sheetName);
                SheetGenerator generator = sheetGenerators.get(sheetType);
                if (generator == null) {
                    throw new IllegalStateException("No sheet generator registered for type: " + sheetType);
                }
                generator.generate(wb, sheetName, rows);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private String normalizeSheetName(String sheetName) {
        if (sheetName == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(sheetName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents.toUpperCase(Locale.ROOT);
    }

    private Integer extractEtapaNumber(String sheetKey) {
        Matcher matcher = ETAPA_PATTERN.matcher(sheetKey);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private List<Map.Entry<String, List<Map<String, Object>>>> orderByDependencies(
            Map<String, List<Map<String, Object>>> etapas
    ) {
        List<Map.Entry<String, List<Map<String, Object>>>> ordered = new ArrayList<>(etapas.entrySet());
        ordered.sort(Comparator.comparingInt(entry -> classifySheetType(entry.getKey()).priority()));
        return ordered;
    }

    private SheetType classifySheetType(String sheetName) {
        String sheetKey = normalizeSheetName(sheetName);
        Integer etapa = extractEtapaNumber(sheetKey);
        String etapa1Keyword = normalizeSheetName(SheetNames.ETAPA_1.displayName())
                .replaceFirst("^ETAPA\\s+\\d+\\.?\\s*", "");

        if (sheetKey.contains(etapa1Keyword) || Integer.valueOf(1).equals(etapa)) {
            return SheetType.ETAPA_1;
        }
        if (Integer.valueOf(2).equals(etapa)) {
            return SheetType.ETAPA_2;
        }
        if (Integer.valueOf(3).equals(etapa)) {
            return SheetType.ETAPA_3;
        }
        if (Integer.valueOf(4).equals(etapa)) {
            return SheetType.ETAPA_4;
        }
        if (Integer.valueOf(5).equals(etapa)) {
            return SheetType.ETAPA_5;
        }
        if (sheetKey.contains(SheetNames.OCORRENCIAS_RISCO.marker())) {
            return SheetType.OCORRENCIA_RISCO;
        }
        throw new IllegalArgumentException("Nome de aba nao suportado para exportacao: " + sheetName);
    }

    @FunctionalInterface
    private interface SheetGenerator {
        void generate(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows);
    }

    private enum SheetType {
        ETAPA_1(1),
        ETAPA_2(2),
        ETAPA_3(3),
        ETAPA_4(4),
        ETAPA_5(5),
        OCORRENCIA_RISCO(6);

        private final int priority;

        SheetType(int priority) {
            this.priority = priority;
        }

        private int priority() {
            return priority;
        }
    }
}
