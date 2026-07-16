package com.example.demo.infrastructure.excel.sheet;

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

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

abstract class AbstractWorksheetTemplateService {

    protected XSSFColor color(byte red, byte green, byte blue) {
        return new XSSFColor(new byte[]{red, green, blue}, null);
    }

    protected XSSFCellStyle createFilledBoldStyle(
            XSSFWorkbook wb,
            XSSFColor fillColor,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            boolean wrapText
    ) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(fillColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        if (horizontalAlignment != null) {
            style.setAlignment(horizontalAlignment);
        }
        if (verticalAlignment != null) {
            style.setVerticalAlignment(verticalAlignment);
        }

        style.setWrapText(wrapText);
        SheetServiceUtils.applyBorders(style);

        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        style.setFont(boldFont);
        return style;
    }

    protected CellStyle createDataStyle(
            XSSFWorkbook wb,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            boolean wrapText
    ) {
        CellStyle style = wb.createCellStyle();

        if (horizontalAlignment != null) {
            style.setAlignment(horizontalAlignment);
        }
        if (verticalAlignment != null) {
            style.setVerticalAlignment(verticalAlignment);
        }

        style.setWrapText(wrapText);
        SheetServiceUtils.applyBorders(style);
        return style;
    }

    protected int appendDataRowsWithExtra(
            XSSFSheet sheet,
            int startRowIndex,
            List<Map<String, Object>> rows,
            int extraEditableRows,
            BiConsumer<Row, Map<String, Object>> rowWriter
    ) {
        int payloadSize = rows == null ? 0 : rows.size();
        int totalRows = payloadSize + Math.max(0, extraEditableRows);

        for (int i = 0; i < totalRows; i++) {
            Map<String, Object> rowData = i < payloadSize ? rows.get(i) : null;
            Row row = sheet.createRow(startRowIndex + i);
            rowWriter.accept(row, rowData);
        }

        return startRowIndex + totalRows;
    }

    protected void createMergedHeaderInRow(
            Row row,
            int startColumn,
            int endColumn,
            String text,
            CellStyle style,
            XSSFSheet sheet
    ) {
        applyStyleToRange(row, startColumn, endColumn, style);
        Cell firstCell = row.getCell(startColumn);
        if (firstCell == null) {
            firstCell = row.createCell(startColumn);
        }
        firstCell.setCellValue(text);

        sheet.addMergedRegion(new CellRangeAddress(
                row.getRowNum(),
                row.getRowNum(),
                startColumn,
                endColumn
        ));
    }

    protected void applyStyleToRange(Row row, int startColumn, int endColumn, CellStyle style) {
        for (int c = startColumn; c <= endColumn; c++) {
            Cell cell = row.getCell(c);
            if (cell == null) {
                cell = row.createCell(c);
            }
            cell.setCellStyle(style);
        }
    }

    protected void applySelectValidation(
            XSSFSheet sheet,
            DataValidationHelper helper,
            String[] options,
            int columnIndex,
            int firstEditableRow,
            int lastEditableRow
    ) {
        if (columnIndex < 0 || options == null || options.length == 0) {
            return;
        }

        SheetServiceUtils.applySelect(
                sheet,
                helper,
                options,
                columnIndex,
                firstEditableRow,
                lastEditableRow
        );
    }

    protected void applyColumnWidthsByHeaders(
            XSSFSheet sheet,
            List<String> headers,
            ToIntFunction<String> widthResolver
    ) {
        for (int i = 0; i < headers.size(); i++) {
            sheet.setColumnWidth(i, widthResolver.applyAsInt(headers.get(i)));
        }
    }

    protected void applyColumnWidths(
            XSSFSheet sheet,
            int columnCount,
            IntUnaryOperator widthResolver
    ) {
        for (int i = 0; i < columnCount; i++) {
            sheet.setColumnWidth(i, widthResolver.applyAsInt(i));
        }
    }
}
