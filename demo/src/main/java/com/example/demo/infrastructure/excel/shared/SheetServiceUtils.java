package com.example.demo.infrastructure.excel.shared;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Locale;

public final class SheetServiceUtils {

    private static final int RANGE_BUFFER_ROWS = 200;
    private static final int MIN_EDITABLE_LAST_ROW = 2000;

    private SheetServiceUtils() {
    }

    public static int computeLastEditableRow(int firstEditableRow, int payloadRows, int extraEditableRows) {
        int rows = Math.max(0, firstEditableRow)
                + Math.max(0, payloadRows)
                + Math.max(0, extraEditableRows)
                + RANGE_BUFFER_ROWS;
        return Math.max(rows, MIN_EDITABLE_LAST_ROW);
    }

    public static void applyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    public static void applySelect(
            XSSFSheet sheet,
            DataValidationHelper helper,
            String[] options,
            int columnIndex,
            int firstRow,
            int lastRow
    ) {
        DataValidationConstraint constraint = helper.createExplicitListConstraint(options);
        CellRangeAddressList addressList = new CellRangeAddressList(firstRow, lastRow, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    public static String resolveSheetName(XSSFWorkbook wb, String marker, String fallback) {
        String key = marker == null ? "" : marker.toUpperCase(Locale.ROOT);
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            String name = wb.getSheetName(i);
            if (name != null && name.toUpperCase(Locale.ROOT).contains(key)) {
                return name;
            }
        }
        return fallback;
    }
}
