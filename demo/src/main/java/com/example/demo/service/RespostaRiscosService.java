package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RespostaRiscosService {

    public void generateSheet(
            XSSFWorkbook wb,
            String sheetName,
            List<Map<String, Object>> rows
    ) {

        XSSFSheet sheet = wb.createSheet(sheetName);

        byte[] rgbYellow = new byte[]{(byte)235,(byte)217,(byte)102};
        XSSFColor headerColor = new XSSFColor(rgbYellow,null);

        int r = 0;

        // ---------- HEADER PRINCIPAL ----------
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

        // ---------- HEADERS ----------
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
        CellStyle textStyle = wb.createCellStyle();
        textStyle.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(dataStyleLeft);

        CellStyle dataStyleCenter = wb.createCellStyle();
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(dataStyleCenter);

        // ---------- DADOS ----------
        for(Map<String,Object> rowData : rows){

            Row dataRow = sheet.createRow(r++);

            Cell cell0 = dataRow.createCell(0);
            cell0.setCellValue(String.valueOf(rowData.getOrDefault("Processo","")));
            cell0.setCellStyle(dataStyleLeft);

            // Coluna 1: Fase
            Cell cell1 = dataRow.createCell(1);
            cell1.setCellValue(String.valueOf(rowData.getOrDefault("Fase","")));
            cell1.setCellStyle(dataStyleCenter);

            // Coluna 2: Evento de Risco
            Cell cell2 = dataRow.createCell(2);
            cell2.setCellValue(String.valueOf(rowData.getOrDefault("Evento de Risco","")));
            cell2.setCellStyle(dataStyleLeft);

            // Coluna 3: Opção de Tratamento (Formatação de Texto)
            Cell cell3 = dataRow.createCell(3);
            String opcao = String.valueOf(rowData.getOrDefault("Opção de Tratamento",""));
            if(!opcao.isEmpty()){
                opcao = opcao.substring(0,1).toUpperCase() + opcao.substring(1).toLowerCase();
            }
            cell3.setCellValue(opcao);
            cell3.setCellStyle(dataStyleCenter);

            // Coluna 4: Justificativa
            Cell cell4 = dataRow.createCell(4);
            cell4.setCellValue(String.valueOf(rowData.getOrDefault("Justificativa da escolha da opção de tratamento","")));
            cell4.setCellStyle(dataStyleLeft);
        }




        // ---------- DROPDOWN ----------
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