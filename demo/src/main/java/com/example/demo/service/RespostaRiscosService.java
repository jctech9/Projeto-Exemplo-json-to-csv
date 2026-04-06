package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RespostaRiscosService {

    private static final String ETAPA_2_SHEET_FALLBACK_NAME = "ETAPA 2. IDENTIF. DE EVENTOS";
    private static final int EXTRA_EDITABLE_ROWS = 10;

    public void generateSheet(
            XSSFWorkbook wb,
            String sheetName,
            List<Map<String, Object>> rows
    ) {

        XSSFSheet sheet = wb.createSheet(sheetName);
        String etapa2SheetName = resolveEtapa2SheetName(wb);

        byte[] rgbYellow = new byte[]{(byte) 255, (byte) 255, (byte) 0};
        XSSFColor headerColor = new XSSFColor(rgbYellow, null);

        int r = 0;

        Row titleRow = sheet.createRow(r++);

        XSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFillForegroundColor(headerColor);
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        applyBorders(titleStyle);

        Font bold = wb.createFont();
        bold.setBold(true);
        titleStyle.setFont(bold);

        for(int c=0;c<=4;c++){
            Cell cell = titleRow.createCell(c);
            if(c==2) cell.setCellValue("Resposta aos Riscos");
            cell.setCellStyle(titleStyle);
        }

        sheet.addMergedRegion(new CellRangeAddress(
                titleRow.getRowNum(),
                titleRow.getRowNum(),
                2,
                4
        ));

        Row headerRow = sheet.createRow(r++);

        String[] headers = {
                "Processo",
                "Fase",
                "Evento de Risco",
                "Opção de Tratamento",
                "Justificativa da escolha da opção de tratamento"
        };

        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(headerColor);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        applyBorders(headerStyle);

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for(int c=0;c<headers.length;c++){
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle dataStyleLeft = wb.createCellStyle();
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(dataStyleLeft);

        CellStyle dataStyleCenter = wb.createCellStyle();
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(dataStyleCenter);

        for(Map<String,Object> rowData : rows){

            Row dataRow = sheet.createRow(r++);
            int rowNum = dataRow.getRowNum() + 1;

            Cell cell0 = dataRow.createCell(0);
            cell0.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!A" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!A" + rowNum + ")");
            cell0.setCellStyle(dataStyleLeft);

            Cell cell1 = dataRow.createCell(1);
            cell1.setCellValue(String.valueOf(rowData.getOrDefault("Fase","")));
            cell1.setCellStyle(dataStyleCenter);

            Cell cell2 = dataRow.createCell(2);
            cell2.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + ")");
            cell2.setCellStyle(dataStyleLeft);

            Cell cell3 = dataRow.createCell(3);
            String opcao = String.valueOf(rowData.getOrDefault("Opção de Tratamento",""));
            if(!opcao.isEmpty()){
                opcao = opcao.substring(0,1).toUpperCase() + opcao.substring(1).toLowerCase();
            }
            cell3.setCellValue(opcao);
            cell3.setCellStyle(dataStyleCenter);


            Cell cell4 = dataRow.createCell(4);
            cell4.setCellValue(String.valueOf(rowData.getOrDefault("Justificativa da escolha da opção de tratamento","")));
            cell4.setCellStyle(dataStyleLeft);
        }

        for (int i = 0; i < EXTRA_EDITABLE_ROWS; i++) {
            Row dataRow = sheet.createRow(r++);
            int rowNum = dataRow.getRowNum() + 1;

            Cell cell0 = dataRow.createCell(0);
            cell0.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!A" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!A" + rowNum + ")");
            cell0.setCellStyle(dataStyleLeft);

            Cell cell1 = dataRow.createCell(1);
            cell1.setCellValue("");
            cell1.setCellStyle(dataStyleCenter);

            Cell cell2 = dataRow.createCell(2);
            cell2.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + ")");
            cell2.setCellStyle(dataStyleLeft);

            Cell cell3 = dataRow.createCell(3);
            cell3.setCellValue("");
            cell3.setCellStyle(dataStyleCenter);

            Cell cell4 = dataRow.createCell(4);
            cell4.setCellValue("");
            cell4.setCellStyle(dataStyleLeft);
        }




        DataValidationHelper helper = sheet.getDataValidationHelper();

        DataValidationConstraint constraint =
                helper.createExplicitListConstraint(OPCOES_TRATAMENTO);

        CellRangeAddressList addressList =
                new CellRangeAddressList(2,1000,3,3);

        DataValidation validation =
                helper.createValidation(constraint,addressList);

        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);

        sheet.addValidationData(validation);

        // ---------- TAMANHO COLUNAS ----------
        sheet.setColumnWidth(0,9000);
        sheet.setColumnWidth(1,6000);
        sheet.setColumnWidth(2,22000);
        sheet.setColumnWidth(3,7000);
        sheet.setColumnWidth(4,50000);
    }

    private String resolveEtapa2SheetName(XSSFWorkbook wb) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            String name = wb.getSheetName(i);
            if (name != null && name.contains("ETAPA 2")) {
                return name;
            }
        }
        return ETAPA_2_SHEET_FALLBACK_NAME;
    }

    private static final String[] OPCOES_TRATAMENTO = {
            "Aceitar",
            "Mitigar",
            "Compartilhar",
            "Evitar"
    };

    private void applyBorders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

}
