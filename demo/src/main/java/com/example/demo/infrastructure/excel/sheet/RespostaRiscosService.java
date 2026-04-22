package com.example.demo.infrastructure.excel.sheet;

import com.example.demo.contracts.RespostaRiscosColumns;
import com.example.demo.contracts.SheetNames;
import com.example.demo.contracts.ValidationOptions;
import com.example.demo.infrastructure.excel.shared.SheetServiceUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RespostaRiscosService {

    private static final int EXTRA_EDITABLE_ROWS = 10;
    private static final List<RespostaRiscosColumns> FIXED_COLUMNS = RespostaRiscosColumns.ordered();

    public void generateSheet(
            XSSFWorkbook wb,
            String sheetName,
            List<Map<String, Object>> rows
    ) {

        XSSFSheet sheet = wb.createSheet(sheetName);
        String etapa2SheetName = SheetServiceUtils.resolveSheetName(
                wb,
                SheetNames.ETAPA_2.marker(),
                SheetNames.ETAPA_2.displayName()
        );
        int firstEditableRow = 2;
        int lastEditableRow = SheetServiceUtils.computeLastEditableRow(firstEditableRow, rows.size(), EXTRA_EDITABLE_ROWS);

        byte[] rgbYellow = new byte[]{(byte) 255, (byte) 255, (byte) 0};
        XSSFColor headerColor = new XSSFColor(rgbYellow, null);

        int r = 0;

        Row titleRow = sheet.createRow(r++);

        XSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFillForegroundColor(headerColor);
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        SheetServiceUtils.applyBorders(titleStyle);

        Font bold = wb.createFont();
        bold.setBold(true);
        titleStyle.setFont(bold);

        for (int c = 0; c <= RespostaRiscosColumns.lastIndex(); c++) {
            Cell cell = titleRow.createCell(c);
            if (c == RespostaRiscosColumns.EVENTO_RISCO.index()) {
                cell.setCellValue("Resposta aos Riscos");
            }
            cell.setCellStyle(titleStyle);
        }

        sheet.addMergedRegion(new CellRangeAddress(
                titleRow.getRowNum(),
                titleRow.getRowNum(),
                RespostaRiscosColumns.EVENTO_RISCO.index(),
                RespostaRiscosColumns.lastIndex()
        ));

        Row headerRow = sheet.createRow(r++);

        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(headerColor);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        SheetServiceUtils.applyBorders(headerStyle);

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (RespostaRiscosColumns column : FIXED_COLUMNS) {
            Cell cell = headerRow.createCell(column.index());
            cell.setCellValue(column.headerLabel());
            cell.setCellStyle(headerStyle);
        }

        CellStyle dataStyleLeft = wb.createCellStyle();
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        SheetServiceUtils.applyBorders(dataStyleLeft);

        CellStyle dataStyleCenter = wb.createCellStyle();
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        SheetServiceUtils.applyBorders(dataStyleCenter);

        int totalRows = rows.size() + EXTRA_EDITABLE_ROWS;
        for (int i = 0; i < totalRows; i++) {
            Map<String, Object> rowData = i < rows.size() ? rows.get(i) : null;
            Row dataRow = sheet.createRow(r++);
            int rowNum = dataRow.getRowNum() + 1;
            populateDataRow(dataRow, rowData, rowNum, etapa2SheetName, dataStyleLeft, dataStyleCenter);
        }

        DataValidationHelper helper = sheet.getDataValidationHelper();
        SheetServiceUtils.applySelect(
                sheet,
                helper,
                ValidationOptions.OPCOES_TRATAMENTO,
                RespostaRiscosColumns.OPCAO_TRATAMENTO.index(),
                firstEditableRow,
                lastEditableRow
        );

        for (RespostaRiscosColumns column : FIXED_COLUMNS) {
            sheet.setColumnWidth(column.index(), column.columnWidth());
        }
    }

    private void populateDataRow(
            Row dataRow,
            Map<String, Object> rowData,
            int rowNum,
            String etapa2SheetName,
            CellStyle dataStyleLeft,
            CellStyle dataStyleCenter
    ) {
        // Processo e Evento de Risco são espelhados da ETAPA 2 para manter vínculo por linha.
        Cell cell0 = dataRow.createCell(RespostaRiscosColumns.PROCESSO.index());
        cell0.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!A" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!A" + rowNum + ")");
        cell0.setCellStyle(dataStyleLeft);

        Cell cell1 = dataRow.createCell(RespostaRiscosColumns.FASE.index());
        cell1.setCellValue(rowData == null ? "" : String.valueOf(rowData.getOrDefault(RespostaRiscosColumns.FASE.key(), "")));
        cell1.setCellStyle(dataStyleCenter);

        Cell cell2 = dataRow.createCell(RespostaRiscosColumns.EVENTO_RISCO.index());
        cell2.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + ")");
        cell2.setCellStyle(dataStyleLeft);

        Cell cell3 = dataRow.createCell(RespostaRiscosColumns.OPCAO_TRATAMENTO.index());
        String rawOpcao = rowData == null ? "" : String.valueOf(rowData.getOrDefault(RespostaRiscosColumns.OPCAO_TRATAMENTO.key(), ""));
        // Força valor canônico para casar com a lista de validação do Excel.
        cell3.setCellValue(normalizeOpcaoTratamento(rawOpcao));
        cell3.setCellStyle(dataStyleCenter);

        Cell cell4 = dataRow.createCell(RespostaRiscosColumns.JUSTIFICATIVA_TRATAMENTO.index());
        cell4.setCellValue(rowData == null ? "" : String.valueOf(rowData.getOrDefault(RespostaRiscosColumns.JUSTIFICATIVA_TRATAMENTO.key(), "")));
        cell4.setCellStyle(dataStyleLeft);
    }

    private String normalizeOpcaoTratamento(String opcao) {
        return ValidationOptions.normalizeOpcaoTratamento(opcao);
    }

}
