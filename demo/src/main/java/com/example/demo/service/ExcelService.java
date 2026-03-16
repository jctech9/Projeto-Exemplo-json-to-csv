package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ExcelService {
    private final DadosProcessoService dadosProcessoService;
    private final AvaliacaoRiscosService avaliacaoRiscosService;
    private final RespostaRiscosService respostaRiscosService;
    private final IdentificacaoEventosService identificacaoEventosService;

    public  ExcelService(DadosProcessoService dadosProcessoService,
                         AvaliacaoRiscosService avaliacaoRiscosService,
                         IdentificacaoEventosService identificacaoEventosService,
                         RespostaRiscosService respostaRiscosService) {
        this.dadosProcessoService = dadosProcessoService;
        this.avaliacaoRiscosService = avaliacaoRiscosService;
        this.identificacaoEventosService = identificacaoEventosService;
        this.respostaRiscosService = respostaRiscosService;
    }

    public byte[] generateXlsx(Map<String, List<Map<String, Object>>> etapas) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (Map.Entry<String, List<Map<String, Object>>> entry : etapas.entrySet()) {

                String sheetName = entry.getKey();
                List<Map<String, Object>> rows = entry.getValue();

                if(sheetName.contains("ETAPA 1")) {
                    dadosProcessoService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                if(sheetName.contains("ETAPA 2")) {
                    identificacaoEventosService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                if(sheetName.contains("ETAPA 3")) {
                    avaliacaoRiscosService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                if(sheetName.contains("ETAPA 4")){
                    respostaRiscosService.generateSheet(wb, sheetName, rows);
                    continue;
                }

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

            wb.write(out);
            return out.toByteArray();
        }
    }
}