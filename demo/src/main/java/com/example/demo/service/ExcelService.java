package com.example.demo.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    }

    public byte[] generateXlsx(Map<String, List<Map<String, Object>>> etapas) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (Map.Entry<String, List<Map<String, Object>>> entry : orderByDependencies(etapas)) {

                String sheetName = entry.getKey();
                List<Map<String, Object>> rows = entry.getValue();
                String sheetKey = normalizeSheetName(sheetName);
                Integer etapa = extractEtapaNumber(sheetKey);

                if (sheetKey.contains("DADOS DO PROCESSO") || Integer.valueOf(1).equals(etapa)) {
                    dadosProcessoService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                if (Integer.valueOf(2).equals(etapa)) {
                    identificacaoEventosService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                if (Integer.valueOf(3).equals(etapa)) {
                    avaliacaoRiscosService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                if (Integer.valueOf(4).equals(etapa)) {
                    respostaRiscosService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                if (Integer.valueOf(5).equals(etapa)) {
                    atividadesControleService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                if (sheetKey.contains("OCORRENCIAS DE RISCO")) {
                    ocorrenciaRiscoService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                createDefaultSheet(wb, sheetName, rows);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void createDefaultSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {

        XSSFSheet sheet = wb.createSheet(sheetName);

        LinkedHashSet<String> headers = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            headers.addAll(row.keySet());
        }

        List<String> headerList = new ArrayList<>(headers);
        int r = 0;

        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        Row headerRow = sheet.createRow(r++);
        for (int c = 0; c < headerList.size(); c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headerList.get(c));
            cell.setCellStyle(headerStyle);
        }

        for (Map<String, Object> rowData : rows) {
            Row dataRow = sheet.createRow(r++);
            for (int c = 0; c < headerList.size(); c++) {
                String columnName = headerList.get(c);
                Cell cell = dataRow.createCell(c);
                Object val = rowData.get(columnName);
                String strVal = val == null ? "" : String.valueOf(val);

                try {
                    double num = Double.parseDouble(strVal.replace(",", "."));
                    cell.setCellValue(num);
                } catch (Exception e) {
                    cell.setCellValue(strVal);
                }
            }
        }

        for (int c = 0; c < headerList.size(); c++) {
            sheet.autoSizeColumn(c);
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
        List<Map.Entry<String, List<Map<String, Object>>>> etapa1Sheets = new ArrayList<>();
        List<Map.Entry<String, List<Map<String, Object>>>> etapa2Sheets = new ArrayList<>();
        List<Map.Entry<String, List<Map<String, Object>>>> etapa3Sheets = new ArrayList<>();
        List<Map.Entry<String, List<Map<String, Object>>>> etapa4Sheets = new ArrayList<>();
        List<Map.Entry<String, List<Map<String, Object>>>> etapa5Sheets = new ArrayList<>();
        List<Map.Entry<String, List<Map<String, Object>>>> ocorrenciaSheets = new ArrayList<>();
        List<Map.Entry<String, List<Map<String, Object>>>> otherSheets = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : etapas.entrySet()) {
            String sheetKey = normalizeSheetName(entry.getKey());
            Integer etapa = extractEtapaNumber(sheetKey);

            if (sheetKey.contains("DADOS DO PROCESSO") || Integer.valueOf(1).equals(etapa)) {
                etapa1Sheets.add(entry);
                continue;
            }
            if (Integer.valueOf(2).equals(etapa)) {
                etapa2Sheets.add(entry);
                continue;
            }
            if (Integer.valueOf(3).equals(etapa)) {
                etapa3Sheets.add(entry);
                continue;
            }
            if (Integer.valueOf(4).equals(etapa)) {
                etapa4Sheets.add(entry);
                continue;
            }
            if (Integer.valueOf(5).equals(etapa)) {
                etapa5Sheets.add(entry);
                continue;
            }
            if (sheetKey.contains("OCORRENCIAS DE RISCO")) {
                ocorrenciaSheets.add(entry);
                continue;
            }
            otherSheets.add(entry);
        }

        List<Map.Entry<String, List<Map<String, Object>>>> ordered = new ArrayList<>(etapas.size());
        ordered.addAll(etapa1Sheets);
        ordered.addAll(etapa2Sheets);
        ordered.addAll(etapa3Sheets);
        ordered.addAll(etapa4Sheets);
        ordered.addAll(etapa5Sheets);
        ordered.addAll(ocorrenciaSheets);
        ordered.addAll(otherSheets);
        return ordered;
    }
}
