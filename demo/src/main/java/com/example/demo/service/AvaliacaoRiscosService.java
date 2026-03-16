package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AvaliacaoRiscosService {

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {

        XSSFSheet sheet = wb.createSheet(sheetName);
        int r = 0;

        byte[] rgbPink = new byte[]{(byte) 230, (byte) 145, (byte) 145};
        XSSFColor npiPink = new XSSFColor(rgbPink, null);

        // ----- estilo rosa -----
        XSSFCellStyle pinkStyle = wb.createCellStyle();
        pinkStyle.setFillForegroundColor(npiPink);
        pinkStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        pinkStyle.setAlignment(HorizontalAlignment.CENTER);
        pinkStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(pinkStyle);

        Font font = wb.createFont();
        font.setBold(true);
        pinkStyle.setFont(font);

        // ----- cabeçalho -----
        Row row1 = sheet.createRow(r++);
        Row row2 = sheet.createRow(r++);

        createVerticalHeader(row1,row2,0,"Evento de Risco",pinkStyle,sheet);
        createMergedGroup(row1,1,5,"Avaliação dos Riscos",pinkStyle,sheet);
        createVerticalHeader(row1,row2,6,"Classificação do Risco Inerente",pinkStyle,sheet);
        createMergedGroup(row1,7,10,"Avaliação dos Controles",pinkStyle,sheet);
        createMergedGroup(row1,11,12,"Risco Residual",pinkStyle,sheet);
        createVerticalHeader(row1,row2,13,"Data da Última Avaliação",pinkStyle,sheet);

        // ----- headers -----
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        for (Map<String,Object> row : rows) headers.addAll(row.keySet());
        List<String> headerList = new ArrayList<>(headers);

        for(int c=0;c<headerList.size();c++){
            Cell cell = row2.getCell(c);
            if(cell==null) cell=row2.createCell(c);
            cell.setCellValue(headerList.get(c));
            cell.setCellStyle(pinkStyle);
        }

        // ----- índices das colunas -----
        int colProb = headerList.indexOf("Probabilidade");
        int colImpacto = headerList.indexOf("Impacto");
        int colRiscoInerente = headerList.indexOf("Risco Inerente (PxI)");
        int colClassInerente = headerList.indexOf("Classificação do Risco Inerente");

        int colAvaliacao = headerList.indexOf("Avaliação dos Controles");
        int colFAC = headerList.indexOf("FAC");

        int colRiscoResidual = headerList.indexOf("Risco Residual");
        int colClassResidual = headerList.indexOf("Classificação do Risco Residual");

        // ----- dados -----
        for(Map<String,Object> rowData : rows){

            Row dataRow = sheet.createRow(r++);
            int rowNum = dataRow.getRowNum()+1;

            for(int c=0;c<headerList.size();c++){

                String columnName = headerList.get(c);
                Cell cell = dataRow.createCell(c);
                Object val=rowData.get(columnName);
                String strVal = val==null?"":String.valueOf(val);

                if(c==colRiscoInerente){
                    String p = colLetter(colProb);
                    String i = colLetter(colImpacto);
                    cell.setCellFormula(p+rowNum+"*"+i+rowNum);
                }

                else if(c==colFAC){
                    String av = colLetter(colAvaliacao);
                    cell.setCellFormula(
                            "IF("+av+rowNum+"=\"Forte\",0.2,"+
                                    "IF("+av+rowNum+"=\"Satisfatório\",0.4,"+
                                    "IF("+av+rowNum+"=\"Mediano\",0.6,"+
                                    "IF("+av+rowNum+"=\"Fraco\",0.8,1))))");
                }

                else if(c==colRiscoResidual){
                    String iner = colLetter(colRiscoInerente);
                    String fac = colLetter(colFAC);
                    cell.setCellFormula(iner+rowNum+"*"+fac+rowNum);
                }

                else if(c==colClassInerente){
                    String iner = colLetter(colRiscoInerente);
                    cell.setCellFormula(
                            "IF("+iner+rowNum+"=0,\"\",IF("+iner+rowNum+
                                    "<10,\"Risco Baixo\",IF("+iner+rowNum+
                                    "<40,\"Risco Médio\",IF("+iner+rowNum+
                                    "<80,\"Risco Alto\",\"Risco Extremo\"))))");
                }

                else if(c==colClassResidual){
                    String res = colLetter(colRiscoResidual);
                    cell.setCellFormula(
                            "IF("+res+rowNum+"=0,\"\",IF("+res+rowNum+
                                    "<10,\"Risco Baixo\",IF("+res+rowNum+
                                    "<40,\"Risco Médio\",IF("+res+rowNum+
                                    "<80,\"Risco Alto\",\"Risco Extremo\"))))");
                }

                else{
                    try{
                        double num = Double.parseDouble(strVal.replace(",","."));
                        cell.setCellValue(num);
                    }catch(Exception e){
                        cell.setCellValue(strVal);
                    }
                }

                CellStyle dataStyle = wb.createCellStyle();
                applyBorders(dataStyle);
                dataStyle.setAlignment(HorizontalAlignment.CENTER);
                cell.setCellStyle(dataStyle);
            }
        }

        applyConditionalFormatting(sheet, headerList);
        setupValidation(sheet, headerList);

        for(int c=0;c<headerList.size();c++) sheet.autoSizeColumn(c);
    }

    // ---------- helpers ----------

    private String colLetter(int col){
        return CellReference.convertNumToColString(col);
    }

    private void applyConditionalFormatting(XSSFSheet sheet,List<String> headers){

        SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();

        int col1=headers.indexOf("Classificação do Risco Inerente");
        int col2=headers.indexOf("Classificação do Risco Residual");

        int[] cols={col1,col2};

        for(int col:cols){
            if(col==-1) continue;

            createRule(scf, sheet, col, "\"Risco Extremo\"", new byte[]{(byte)255,0,0});
            createRule(scf, sheet, col, "\"Risco Alto\"", new byte[]{(byte)255,(byte)192,0});
            createRule(scf, sheet, col, "\"Risco Médio\"", new byte[]{(byte)255,(byte)255,0});
            createRule(scf, sheet, col, "\"Risco Baixo\"", new byte[]{(byte)146,(byte)208,80});
        }
    }

    private void createRule(SheetConditionalFormatting scf, XSSFSheet sheet, int col, String value, byte[] rgb){

        ConditionalFormattingRule rule =
                scf.createConditionalFormattingRule(ComparisonOperator.EQUAL, value);

        PatternFormatting pattern = rule.createPatternFormatting();
        pattern.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        // cria a cor corretamente para conditional formatting
        XSSFColor color = new XSSFColor(rgb, null);

        ((XSSFConditionalFormattingRule) rule)
                .getPatternFormatting()
                .setFillBackgroundColor(color);

        CellRangeAddress[] regions = { new CellRangeAddress(2,1000,col,col) };

        scf.addConditionalFormatting(regions, rule);
    }

    private void setupValidation(XSSFSheet sheet,List<String> headers){

        DataValidationHelper helper=sheet.getDataValidationHelper();

        int impacto=headers.indexOf("Impacto");
        int controle=headers.indexOf("Avaliação dos Controles");

        if(impacto!=-1){
            applySelect(sheet,helper,
                    new String[]{"Muito baixo","Baixo","Médio","Alto","Muito alto"},
                    impacto);
        }

        if(controle!=-1){
            applySelect(sheet,helper,
                    new String[]{"Inexistente","Fraco","Mediano","Satisfatório","Forte"},
                    controle);
        }
    }

    private void applySelect(XSSFSheet sheet,DataValidationHelper helper,String[] opts,int col){

        DataValidationConstraint c=helper.createExplicitListConstraint(opts);

        CellRangeAddressList addr=new CellRangeAddressList(2,1000,col,col);

        DataValidation v=helper.createValidation(c,addr);
        v.setSuppressDropDownArrow(true);
        v.setShowErrorBox(true);

        sheet.addValidationData(v);
    }

    private void applyBorders(CellStyle s){
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private void createVerticalHeader(Row r1,Row r2,int col,String text,CellStyle style,XSSFSheet sheet){

        Cell cell=r1.createCell(col);
        cell.setCellValue(text);
        cell.setCellStyle(style);

        if(r2.getCell(col)==null) r2.createCell(col).setCellStyle(style);

        sheet.addMergedRegion(new CellRangeAddress(r1.getRowNum(),r1.getRowNum()+1,col,col));
    }

    private void createMergedGroup(Row row,int start,int end,String text,CellStyle style,XSSFSheet sheet){

        for(int i=start;i<=end;i++){
            Cell cell=row.createCell(i);
            if(i==start) cell.setCellValue(text);
            cell.setCellStyle(style);
        }

        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(),row.getRowNum(),start,end));
    }
}