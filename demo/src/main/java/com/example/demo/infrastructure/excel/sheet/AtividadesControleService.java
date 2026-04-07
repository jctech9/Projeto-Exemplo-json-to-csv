package com.example.demo.infrastructure.excel.sheet;

import com.example.demo.contracts.AtividadesControleColumns;
import com.example.demo.contracts.SheetNames;
import com.example.demo.contracts.ValidationOptions;
import com.example.demo.infrastructure.excel.shared.SheetServiceUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
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
public class AtividadesControleService {

    private static final int EXTRA_EDITABLE_ROWS = 10;
    private static final List<AtividadesControleColumns> FIXED_COLUMNS = AtividadesControleColumns.ordered();

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {
        XSSFSheet sheet = wb.createSheet(sheetName);
        String etapa2SheetName = SheetServiceUtils.resolveSheetName(
                wb,
                SheetNames.ETAPA_2.marker(),
                SheetNames.ETAPA_2.displayName()
        );
        String etapa4SheetName = SheetServiceUtils.resolveSheetName(
                wb,
                SheetNames.ETAPA_4.marker(),
                SheetNames.ETAPA_4.displayName()
        );
        int firstEditableRow = 2;
        int lastEditableRow = SheetServiceUtils.computeLastEditableRow(firstEditableRow, rows.size(), EXTRA_EDITABLE_ROWS);

        // Cor Cinza para o Cabeçalho
        byte[] rgbGrey = new byte[]{(byte) 217, (byte) 217, (byte) 217};
        XSSFColor npiGrey = new XSSFColor(rgbGrey, null);

        int r = 0;

        // 1. Estilos de Cabeçalho e Dados
        XSSFCellStyle greyStyle = wb.createCellStyle();
        greyStyle.setFillForegroundColor(npiGrey);
        greyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        greyStyle.setAlignment(HorizontalAlignment.CENTER);
        greyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        greyStyle.setWrapText(true);
        SheetServiceUtils.applyBorders(greyStyle);
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        greyStyle.setFont(boldFont);

        CellStyle dataStyleLeft = wb.createCellStyle();
        SheetServiceUtils.applyBorders(dataStyleLeft);
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

        CellStyle dataStyleCenter = wb.createCellStyle();
        SheetServiceUtils.applyBorders(dataStyleCenter);
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);

        // 2. Criar Cabeçalho Duplo (Linha 1: Títulos de Grupo)
        Row row1 = sheet.createRow(r++);
        Row row2 = sheet.createRow(r++);
        row2.setHeightInPoints(35);

        // Plano de Tratamento (Colunas A até G)
        createGroupHeader(row1, 0, 7, "Plano de Tratamento", greyStyle, sheet);
        // Plano de Contingência (Colunas H até J)
        createGroupHeader(row1, 8, 10, "Plano de Contingência", greyStyle, sheet);

        // 3. Gerar Nomes das Colunas (Linha 2)
        for (AtividadesControleColumns column : FIXED_COLUMNS) {
            Cell cell = row2.createCell(column.index());
            cell.setCellValue(column.headerLabel());
            cell.setCellStyle(greyStyle);
        }

        // 4. Preenchimento de Dados (linhas do payload + linhas extras editáveis)
        int totalRows = rows.size() + EXTRA_EDITABLE_ROWS;
        for (int i = 0; i < totalRows; i++) {
            Map<String, Object> rowData = i < rows.size() ? rows.get(i) : null;
            Row dataRow = sheet.createRow(r++);
            int rowNum = dataRow.getRowNum() + 1;

            for (AtividadesControleColumns column : FIXED_COLUMNS) {
                Cell cell = dataRow.createCell(column.index());

                if (column == AtividadesControleColumns.EVENTO_RISCO) {
                    String formula = "IF('" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + ")";
                    cell.setCellFormula(formula);
                    cell.setCellStyle(dataStyleLeft);
                } else if (column == AtividadesControleColumns.OPCAO_TRATAMENTO) {
                    String formula = "IF('" + etapa4SheetName.replace("'", "''") + "'!D" + rowNum + "=\"\",\"\",'" + etapa4SheetName.replace("'", "''") + "'!D" + rowNum + ")";
                    cell.setCellFormula(formula);
                    cell.setCellStyle(dataStyleCenter);
                } else {
                    Object val = rowData == null ? null : rowData.get(column.key());
                    cell.setCellValue(val == null ? "" : String.valueOf(val));
                    if (column.leftAligned()) {
                        cell.setCellStyle(dataStyleLeft);
                    } else {
                        cell.setCellStyle(dataStyleCenter);
                    }
                }
            }
        }

        // 5. Formatação Condicional para Status
        applyStatusFormatting(sheet, firstEditableRow, lastEditableRow);

        // 6. Selects e Larguras
        setupValidations(sheet, firstEditableRow, lastEditableRow);
        setColumnWidths(sheet);
    }

    private void applyStatusFormatting(XSSFSheet sheet, int firstEditableRow, int lastEditableRow) {
        int colStatus = AtividadesControleColumns.STATUS.index();

        SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();

        // Implementado -> Verde
        createRule(scf, colStatus, "\"" + ValidationOptions.STATUS_IMPLEMENTACAO[2] + "\"", IndexedColors.BRIGHT_GREEN.getIndex(), firstEditableRow, lastEditableRow);
        // Em implementação -> Amarelo
        createRule(scf, colStatus, "\"" + ValidationOptions.STATUS_IMPLEMENTACAO[1] + "\"", IndexedColors.YELLOW.getIndex(), firstEditableRow, lastEditableRow);
        // Não implementado -> Vermelho
        createRule(scf, colStatus, "\"" + ValidationOptions.STATUS_IMPLEMENTACAO[0] + "\"", IndexedColors.RED.getIndex(), firstEditableRow, lastEditableRow);
    }

    private void createRule(
            SheetConditionalFormatting scf,
            int col,
            String value,
            short colorIndex,
            int firstEditableRow,
            int lastEditableRow
    ) {
        ConditionalFormattingRule rule = scf.createConditionalFormattingRule(ComparisonOperator.EQUAL, value);
        PatternFormatting fill = rule.createPatternFormatting();
        fill.setFillForegroundColor(colorIndex);
        fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        CellRangeAddress[] regions = {new CellRangeAddress(firstEditableRow, lastEditableRow, col, col)};
        scf.addConditionalFormatting(regions, rule);
    }

    private void createGroupHeader(Row row, int start, int end, String text, CellStyle style, XSSFSheet sheet) {
        Cell cell = row.createCell(start);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        for (int i = start + 1; i <= end; i++) row.createCell(i).setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), start, end));
    }

    private void setupValidations(XSSFSheet sheet, int firstEditableRow, int lastEditableRow) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        SheetServiceUtils.applySelect(
                sheet,
                helper,
                ValidationOptions.STATUS_IMPLEMENTACAO,
                AtividadesControleColumns.STATUS.index(),
                firstEditableRow,
                lastEditableRow
        );
    }

    private void setColumnWidths(XSSFSheet sheet) {
        for (AtividadesControleColumns column : FIXED_COLUMNS) {
            sheet.setColumnWidth(column.index(), column.columnWidth());
        }
    }

}


