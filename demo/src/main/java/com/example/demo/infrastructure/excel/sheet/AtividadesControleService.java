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
public class AtividadesControleService extends AbstractWorksheetTemplateService {

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
        XSSFColor npiGrey = color((byte) 217, (byte) 217, (byte) 217);

        int r = 0;

        // 1. Estilos de Cabeçalho e Dados
        XSSFCellStyle greyStyle = createFilledBoldStyle(
            wb,
            npiGrey,
            HorizontalAlignment.CENTER,
            VerticalAlignment.CENTER,
            true
        );

        CellStyle dataStyleLeft = createDataStyle(
            wb,
            HorizontalAlignment.LEFT,
            null,
            false
        );

        CellStyle dataStyleCenter = createDataStyle(
            wb,
            HorizontalAlignment.CENTER,
            null,
            false
        );

        // 2. Criar Cabeçalho Duplo (Linha 1: Títulos de Grupo)
        Row row1 = sheet.createRow(r++);
        Row row2 = sheet.createRow(r++);
        row2.setHeightInPoints(35);

        // Plano de Tratamento (Colunas A até G)
        createMergedHeaderInRow(row1, 0, 7, "Plano de Tratamento", greyStyle, sheet);
        // Plano de Contingência (Colunas H até J)
        createMergedHeaderInRow(row1, 8, 10, "Plano de Contingência", greyStyle, sheet);

        // 3. Gerar Nomes das Colunas (Linha 2)
        for (AtividadesControleColumns column : FIXED_COLUMNS) {
            Cell cell = row2.createCell(column.index());
            cell.setCellValue(column.headerLabel());
            cell.setCellStyle(greyStyle);
        }

        // 4. Preenchimento de Dados (linhas do payload + linhas extras editáveis)
        r = appendDataRowsWithExtra(
                sheet,
                r,
                rows,
                EXTRA_EDITABLE_ROWS,
                (dataRow, rowData) -> {
                    int rowNum = dataRow.getRowNum() + 1;
                    populateDataRow(
                            dataRow,
                            rowData,
                            rowNum,
                            etapa2SheetName,
                            etapa4SheetName,
                            dataStyleLeft,
                            dataStyleCenter
                    );
                }
        );

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
        // Comparação exata evita falsos positivos em textos parecidos de status.
        ConditionalFormattingRule rule = scf.createConditionalFormattingRule(ComparisonOperator.EQUAL, value);
        PatternFormatting fill = rule.createPatternFormatting();
        fill.setFillForegroundColor(colorIndex);
        fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        CellRangeAddress[] regions = {new CellRangeAddress(firstEditableRow, lastEditableRow, col, col)};
        scf.addConditionalFormatting(regions, rule);
    }

    private void setupValidations(XSSFSheet sheet, int firstEditableRow, int lastEditableRow) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        applySelectValidation(
                sheet,
                helper,
                ValidationOptions.STATUS_IMPLEMENTACAO,
                AtividadesControleColumns.STATUS.index(),
                firstEditableRow,
                lastEditableRow
        );
    }

    private void setColumnWidths(XSSFSheet sheet) {
        applyColumnWidths(sheet, FIXED_COLUMNS.size(), index -> FIXED_COLUMNS.get(index).columnWidth());
    }

    private void populateDataRow(
            Row dataRow,
            Map<String, Object> rowData,
            int rowNum,
            String etapa2SheetName,
            String etapa4SheetName,
            CellStyle dataStyleLeft,
            CellStyle dataStyleCenter
    ) {
        for (AtividadesControleColumns column : FIXED_COLUMNS) {
            Cell cell = dataRow.createCell(column.index());

            if (column == AtividadesControleColumns.EVENTO_RISCO) {
                // Evento replica ETAPA 2 para manter o plano de ação alinhado ao risco original.
                String formula = "IF('" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + ")";
                cell.setCellFormula(formula);
                cell.setCellStyle(dataStyleLeft);
            } else if (column == AtividadesControleColumns.OPCAO_TRATAMENTO) {
                // Opção de tratamento vem da ETAPA 4 para evitar divergência manual entre abas.
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

}


