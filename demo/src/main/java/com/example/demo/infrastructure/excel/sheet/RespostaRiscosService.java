package com.example.demo.infrastructure.excel.sheet;

import com.example.demo.contracts.RespostaRiscosColumns;
import com.example.demo.contracts.SheetNames;
import com.example.demo.contracts.ValidationOptions;
import com.example.demo.infrastructure.excel.shared.SheetServiceUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RespostaRiscosService extends AbstractWorksheetTemplateService {

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

        XSSFColor headerColor = color((byte) 255, (byte) 255, (byte) 0);

        int r = 0;

        Row titleRow = sheet.createRow(r++);

        XSSFCellStyle titleStyle = createFilledBoldStyle(
            wb,
            headerColor,
            HorizontalAlignment.CENTER,
            null,
            false
        );
        applyStyleToRange(titleRow, 0, RespostaRiscosColumns.lastIndex(), titleStyle);
        createMergedHeaderInRow(
            titleRow,
            RespostaRiscosColumns.EVENTO_RISCO.index(),
            RespostaRiscosColumns.lastIndex(),
            "Resposta aos Riscos",
            titleStyle,
            sheet
        );

        Row headerRow = sheet.createRow(r++);

        XSSFCellStyle headerStyle = createFilledBoldStyle(
            wb,
            headerColor,
            HorizontalAlignment.CENTER,
            null,
            false
        );

        for (RespostaRiscosColumns column : FIXED_COLUMNS) {
            Cell cell = headerRow.createCell(column.index());
            cell.setCellValue(column.headerLabel());
            cell.setCellStyle(headerStyle);
        }

        CellStyle dataStyleLeft = createDataStyle(
            wb,
            HorizontalAlignment.LEFT,
            VerticalAlignment.CENTER,
            false
        );

        CellStyle dataStyleCenter = createDataStyle(
            wb,
            HorizontalAlignment.CENTER,
            VerticalAlignment.CENTER,
            false
        );

        r = appendDataRowsWithExtra(
            sheet,
            r,
            rows,
            EXTRA_EDITABLE_ROWS,
            (dataRow, rowData) -> {
                int rowNum = dataRow.getRowNum() + 1;
                populateDataRow(dataRow, rowData, rowNum, etapa2SheetName, dataStyleLeft, dataStyleCenter);
            }
        );

        DataValidationHelper helper = sheet.getDataValidationHelper();
        applySelectValidation(
                sheet,
                helper,
                ValidationOptions.OPCOES_TRATAMENTO,
                RespostaRiscosColumns.OPCAO_TRATAMENTO.index(),
                firstEditableRow,
                lastEditableRow
        );

        applyColumnWidths(sheet, FIXED_COLUMNS.size(), index -> FIXED_COLUMNS.get(index).columnWidth());
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
