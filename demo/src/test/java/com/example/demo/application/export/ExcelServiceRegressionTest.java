package com.example.demo.application.export;

import com.example.demo.infrastructure.excel.sheet.AtividadesControleService;
import com.example.demo.infrastructure.excel.sheet.AvaliacaoRiscosService;
import com.example.demo.infrastructure.excel.sheet.DadosProcessoService;
import com.example.demo.infrastructure.excel.sheet.IdentificacaoEventosService;
import com.example.demo.infrastructure.excel.sheet.OcorrenciaRiscoService;
import com.example.demo.infrastructure.excel.sheet.RespostaRiscosService;

import org.apache.poi.ss.usermodel.ConditionalFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExcelServiceRegressionTest {

    @Test
    void shouldResolveCrossSheetReferencesWithCustomNamesAndShuffledInputOrder() throws Exception {
        String etapa1Name = "ETAPA 1 PROCESSO BASE";
        String etapa2Name = "ETAPA 2 EVENTOS D'RISCO";
        String etapa4Name = "ETAPA 4 RESPOSTA";
        String etapa5Name = "ETAPA 5 CONTROLE";

        Map<String, List<Map<String, Object>>> etapas = new LinkedHashMap<>();
        etapas.put(etapa5Name, oneEmptyRow());
        etapas.put(etapa4Name, oneEmptyRow());
        etapas.put(etapa2Name, oneEmptyRow());
        etapas.put(etapa1Name, oneEmptyRow());

        byte[] bytes = buildService().generateXlsx(etapas);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet etapa4Sheet = wb.getSheet(etapa4Name);
            Sheet etapa5Sheet = wb.getSheet(etapa5Name);
            assertNotNull(etapa4Sheet);
            assertNotNull(etapa5Sheet);

            String escapedEtapa2 = escapeSheetName(etapa2Name);
            String escapedEtapa4 = escapeSheetName(etapa4Name);

            assertEquals(
                    "IF('" + escapedEtapa2 + "'!A3=\"\",\"\",'" + escapedEtapa2 + "'!A3)",
                    etapa4Sheet.getRow(2).getCell(0).getCellFormula()
            );
            assertEquals(
                    "IF('" + escapedEtapa2 + "'!C3=\"\",\"\",'" + escapedEtapa2 + "'!C3)",
                    etapa5Sheet.getRow(2).getCell(0).getCellFormula()
            );
            assertEquals(
                    "IF('" + escapedEtapa4 + "'!D3=\"\",\"\",'" + escapedEtapa4 + "'!D3)",
                    etapa5Sheet.getRow(2).getCell(1).getCellFormula()
            );
        }
    }

    @Test
    void shouldUseResolvedEtapa1NameInProcessReferenceFormula() throws Exception {
        String etapa1Name = "ETAPA 1 PROCESSO CUSTOM";
        String etapa2Name = "ETAPA 2 EVENTOS CUSTOM";

        Map<String, List<Map<String, Object>>> etapas = new LinkedHashMap<>();
        etapas.put(etapa2Name, oneEmptyRow());
        etapas.put(etapa1Name, oneEmptyRow());

        byte[] bytes = buildService().generateXlsx(etapas);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet etapa2Sheet = wb.getSheet(etapa2Name);
            assertNotNull(etapa2Sheet);

            String escapedEtapa1 = escapeSheetName(etapa1Name);
            assertEquals(
                    "'" + escapedEtapa1 + "'!A$5",
                    etapa2Sheet.getRow(2).getCell(0).getCellFormula()
            );
        }
    }

    @Test
    void shouldApplyStatusConditionalFormattingUsingForegroundColors() throws Exception {
        String etapa5Name = "ETAPA 5 CONTROLE";
        Map<String, List<Map<String, Object>>> etapas = new LinkedHashMap<>();
        etapas.put(etapa5Name, oneEmptyRow());

        byte[] bytes = buildService().generateXlsx(etapas);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet etapa5Sheet = wb.getSheet(etapa5Name);
            assertNotNull(etapa5Sheet);

            SheetConditionalFormatting scf = etapa5Sheet.getSheetConditionalFormatting();
            assertEquals(3, scf.getNumConditionalFormattings());

            Set<Short> foregroundColors = new LinkedHashSet<>();
            for (int i = 0; i < scf.getNumConditionalFormattings(); i++) {
                ConditionalFormatting formatting = scf.getConditionalFormattingAt(i);
                ConditionalFormattingRule rule = formatting.getRule(0);
                PatternFormatting pattern = rule.getPatternFormatting();
                assertNotNull(pattern);
                assertEquals(PatternFormatting.SOLID_FOREGROUND, pattern.getFillPattern());
                foregroundColors.add(pattern.getFillForegroundColor());
            }

            Set<Short> expectedColors = new HashSet<>();
            expectedColors.add(IndexedColors.BRIGHT_GREEN.getIndex());
            expectedColors.add(IndexedColors.YELLOW.getIndex());
            expectedColors.add(IndexedColors.RED.getIndex());
            assertEquals(expectedColors, foregroundColors);
        }
    }

    private ExcelService buildService() {
        return new ExcelService(
                new DadosProcessoService(),
                new IdentificacaoEventosService(),
                new AvaliacaoRiscosService(),
                new RespostaRiscosService(),
                new AtividadesControleService(),
                new OcorrenciaRiscoService()
        );
    }

    private List<Map<String, Object>> oneEmptyRow() {
        return List.of(new LinkedHashMap<>());
    }

    private String escapeSheetName(String sheetName) {
        return sheetName.replace("'", "''");
    }
}
