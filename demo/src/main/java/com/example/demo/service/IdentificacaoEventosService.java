package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IdentificacaoEventosService {

    private static final String PROCESSO_REFERENCE_FORMULA = "'ETAPA 1. DADOS DO PROCESSO'!A$5";

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {
        XSSFSheet sheet = wb.createSheet(sheetName);

        // COR AZUL PERSONALIZADA
        byte[] rgbBlue = new byte[]{(byte) 180, (byte) 198, (byte) 231};
        XSSFColor npiBlue = new XSSFColor(rgbBlue, null);

        int r = 0;

        // 1. Estilo para o Cabeçalho Azul
        XSSFCellStyle blueStyle = wb.createCellStyle();
        blueStyle.setFillForegroundColor(npiBlue);
        blueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        blueStyle.setAlignment(HorizontalAlignment.CENTER);
        blueStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(blueStyle);
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        blueStyle.setFont(boldFont);
        blueStyle.setWrapText(true);

        // 2. Criar Linha 1: Título Mesclado
        Row titleRow = sheet.createRow(r++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Identificação e Categorização de Riscos");
        titleCell.setCellStyle(blueStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        for(int i = 1; i <= 7; i++) titleRow.createCell(i).setCellStyle(blueStyle);

        // 3. Gerar Nomes das Colunas
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) headers.addAll(row.keySet());
        List<String> headerList = new ArrayList<>(headers);

        Row headerRow = sheet.createRow(r++);
        headerRow.setHeightInPoints(35);
        for (int c = 0; c < headerList.size(); c++) {
            Cell cell = headerRow.createCell(c);
            String headerName = headerList.get(c);

            if (headerName.equalsIgnoreCase("Evento de Risco (indicar)")) {
                cell.setCellValue("Evento de Risco\n(indicar)");
            } else if (headerName.equalsIgnoreCase("Causas (descrever)")) {
                cell.setCellValue("Causas\n(descrever)");
            } else if (headerName.equalsIgnoreCase("Consequências (descrever)")) {
                cell.setCellValue("Consequências\n(descrever)");
            } else {
                cell.setCellValue(headerName);
            }

            cell.setCellStyle(blueStyle);


        }

        // 4. DEFINIÇÃO DOS ESTILOS DE DADOS (Criados uma vez para evitar corromper o arquivo)

        CellStyle dataStyleCenter = wb.createCellStyle();
        applyBorders(dataStyleCenter);
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyleCenter.setWrapText(false);



        CellStyle dataStyleLeft = wb.createCellStyle();
        applyBorders(dataStyleLeft);
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyleLeft.setWrapText(false);

        // 5. Preenchimento de Dados com Lógica de Alinhamento
        for (Map<String, Object> rowData : rows) {
            Row dataRow = sheet.createRow(r++);
            for (int c = 0; c < headerList.size(); c++) {
                String headerName = headerList.get(c);
                Cell cell = dataRow.createCell(c);
                boolean isProcessoColumn = headerName.equalsIgnoreCase("Processo");

                if (isProcessoColumn) {
                    cell.setCellFormula(PROCESSO_REFERENCE_FORMULA);
                } else {
                    Object val = rowData.get(headerName);
                    String text = val == null ? "" : String.valueOf(val);

                    if(!text.isEmpty()){
                        text = text.substring(0,1).toUpperCase() + text.substring(1).toLowerCase();
                    }
                    if (text.equalsIgnoreCase("Ameaca")) {
                        text = "Ameaça";
                    }

                    cell.setCellValue(text);
                }

                // Aplica o estilo baseado no nome da coluna
                if (isProcessoColumn ||
                        headerName.contains("Evento") ||
                        headerName.contains("Causas") ||
                        headerName.contains("Consequências")) {
                    cell.setCellStyle(dataStyleLeft);
                } else {
                    cell.setCellStyle(dataStyleCenter);
                }
            }
        }

        setupValidations(wb, sheet, headerList);
        setColumnWidths(sheet, headerList);
    }

    private void setupValidations(XSSFWorkbook wb, XSSFSheet sheet, List<String> headers) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        int colTipo = headers.indexOf("Tipo de Risco");
        int colCat = headers.indexOf("Categoria");
        int colIntegridade = headers.indexOf("Tipo de Risco de Integridade");


        if (colTipo != -1) applySelect(sheet, helper, new String[]{"Ameaça", "Oportunidade"}, colTipo);
        if (colCat != -1) applySelect(sheet, helper, new String[]{"Estratégico", "Financeiro/orçamentário", "Operacionais", "Legal/de conformidade", "Imagem/reputação", "Integridade"}, colCat);
        if (colIntegridade != -1) applySelect(sheet, helper, new String[]{"Corrupção", "Fraude", "Desvio de conduta"}, colIntegridade);
    }

    private void applySelect(XSSFSheet sheet, DataValidationHelper helper, String[] opts, int col) {
        DataValidationConstraint c = helper.createExplicitListConstraint(opts);
        CellRangeAddressList addr = new CellRangeAddressList(2, 1000, col, col);
        DataValidation v = helper.createValidation(c, addr);
        v.setSuppressDropDownArrow(true);
        v.setShowErrorBox(true);
        sheet.addValidationData(v);
    }

    private void setColumnWidths(XSSFSheet sheet, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h.contains("Evento") || h.contains("Causas")) {
                sheet.setColumnWidth(i, 15000);
            } else if (h.equalsIgnoreCase("Processo")) {
                sheet.setColumnWidth(i, 12000);
            }
            else if (h.contains("Consequências")){
                sheet.setColumnWidth(i, 40000);
            }else {
                sheet.setColumnWidth(i, 7000);
            }
        }
    }

    private void applyBorders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}