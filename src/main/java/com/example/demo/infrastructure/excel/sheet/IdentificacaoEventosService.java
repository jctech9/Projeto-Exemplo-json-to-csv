package com.example.demo.infrastructure.excel.sheet;

import com.example.demo.contracts.SheetNames;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class IdentificacaoEventosService extends AbstractWorksheetTemplateService {

    private static final int EXTRA_EDITABLE_ROWS = 10;
    private static final String META_ROW_TYPE_KEY = "__meta_row_type";
    private static final String META_ROW_TYPE_OPTIONS = "etapa2_options";
    private static final String META_CATEGORIA_OPTIONS_KEY = "__meta_categoria_options";
    private static final List<String> FIXED_HEADERS = Arrays.asList(
            "Processo",
            "Fase",
            "Evento de Risco (indicar)",
            "Tipo de Risco",
            "Categoria",
            "Tipo de Risco de Integridade",
            "Causas (descrever)",
            "Consequências (descrever)"
    );

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {
        PreparedRows preparedRows = prepareRows(rows);
        List<Map<String, Object>> dataRows = preparedRows.dataRows;
        List<String> categoriaOptions = preparedRows.categoriaOptions;

        XSSFSheet sheet = wb.createSheet(sheetName);
        String etapa1SheetName = SheetServiceUtils.resolveSheetName(
                wb,
                SheetNames.ETAPA_1.marker(),
                SheetNames.ETAPA_1.displayName()
        );
        String processoReferenceFormula = buildProcessoReferenceFormula(etapa1SheetName);
        int firstEditableRow = 2;
        int lastEditableRow = SheetServiceUtils.computeLastEditableRow(firstEditableRow, dataRows.size(), EXTRA_EDITABLE_ROWS);

        // COR AZUL PERSONALIZADA
        XSSFColor npiBlue = color((byte) 180, (byte) 198, (byte) 231);

        int r = 0;

        // 1. Estilo para o Cabeçalho Azul
        XSSFCellStyle blueStyle = createFilledBoldStyle(
            wb,
            npiBlue,
            HorizontalAlignment.CENTER,
            VerticalAlignment.CENTER,
            true
        );

        // 2. Criar Linha 1: Título Mesclado
        Row titleRow = sheet.createRow(r++);
        createMergedHeaderInRow(titleRow, 0, 7, "Identificação e Categorização de Riscos", blueStyle, sheet);

        // 3. Gerar Nomes das Colunas
        List<String> headerList = FIXED_HEADERS;

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

        // 4. DEFINIÇÃO DOS ESTILOS DE DADOS
        CellStyle dataStyleCenter = createDataStyle(
                wb,
                HorizontalAlignment.CENTER,
                VerticalAlignment.CENTER,
                false
        );

        CellStyle dataStyleLeft = createDataStyle(
                wb,
                HorizontalAlignment.LEFT,
                VerticalAlignment.CENTER,
                false
        );

        // 5. Preenchimento de Dados com Lógica de Alinhamento
        r = appendDataRowsWithExtra(
                sheet,
                r,
                dataRows,
                EXTRA_EDITABLE_ROWS,
                (dataRow, rowData) -> populateDataRow(
                        headerList,
                        dataRow,
                        rowData,
                        processoReferenceFormula,
                        dataStyleLeft,
                        dataStyleCenter
                )
        );

        setupValidations(sheet, headerList, firstEditableRow, lastEditableRow, categoriaOptions);
        setColumnWidths(sheet, headerList);
    }

    private void setupValidations(
            XSSFSheet sheet,
            List<String> headers,
            int firstEditableRow,
            int lastEditableRow,
            List<String> categoriaOptions
    ) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        int colTipo = headers.indexOf("Tipo de Risco");
        int colCat = headers.indexOf("Categoria");
        int colIntegridade = headers.indexOf("Tipo de Risco de Integridade");

        applySelectValidation(
            sheet,
            helper,
            new String[]{"Ameaça", "Oportunidade"},
            colTipo,
            firstEditableRow,
            lastEditableRow
        );
        if (colCat != -1 && categoriaOptions != null && !categoriaOptions.isEmpty()) {
            applySelectValidation(
                    sheet,
                    helper,
                    categoriaOptions.toArray(new String[0]),
                    colCat,
                    firstEditableRow,
                    lastEditableRow
            );
        }
        applySelectValidation(
            sheet,
            helper,
            new String[]{"Corrupção", "Fraude", "Desvio de conduta"},
            colIntegridade,
            firstEditableRow,
            lastEditableRow
        );
    }

    private PreparedRows prepareRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> dataRows = new ArrayList<>();
        LinkedHashSet<String> categoriaOptions = new LinkedHashSet<>();
        if (rows == null || rows.isEmpty()) {
            return new PreparedRows(dataRows, new ArrayList<>(categoriaOptions));
        }

        for (Map<String, Object> row : rows) {
            // Linhas de metadado são injetadas no builder para transportar opções dinâmicas de select.
            if (isMetadataRow(row)) {
                collectCategoriaOptionsFromMetadata(row, categoriaOptions);
                continue;
            }

            if (row == null) {
                continue;
            }

            dataRows.add(row);
            collectCategoriaOptionsFromData(row, categoriaOptions);
        }

        return new PreparedRows(dataRows, new ArrayList<>(categoriaOptions));
    }

    private boolean isMetadataRow(Map<String, Object> row) {
        if (row == null) {
            return false;
        }
        Object rowType = row.get(META_ROW_TYPE_KEY);
        return META_ROW_TYPE_OPTIONS.equals(rowType);
    }

    private void collectCategoriaOptionsFromMetadata(Map<String, Object> row, LinkedHashSet<String> categoriaOptions) {
        Object value = row.get(META_CATEGORIA_OPTIONS_KEY);
        if (!(value instanceof List<?> list)) {
            return;
        }

        for (Object item : list) {
            if (item == null) {
                continue;
            }
            String option = String.valueOf(item).trim();
            if (!option.isBlank()) {
                categoriaOptions.add(option);
            }
        }
    }

    private void collectCategoriaOptionsFromData(Map<String, Object> row, LinkedHashSet<String> categoriaOptions) {
        Object categoria = row.get("Categoria");
        if (categoria == null) {
            return;
        }

        String option = String.valueOf(categoria).trim();
        if (!option.isBlank()) {
            categoriaOptions.add(option);
        }
    }

    private static final class PreparedRows {
        private final List<Map<String, Object>> dataRows;
        private final List<String> categoriaOptions;

        private PreparedRows(List<Map<String, Object>> dataRows, List<String> categoriaOptions) {
            this.dataRows = dataRows;
            this.categoriaOptions = categoriaOptions;
        }
    }

    private void populateDataRow(
            List<String> headerList,
            Row dataRow,
            Map<String, Object> rowData,
            String processoReferenceFormula,
            CellStyle dataStyleLeft,
            CellStyle dataStyleCenter
    ) {
        for (int c = 0; c < headerList.size(); c++) {
            String headerName = headerList.get(c);
            Cell cell = dataRow.createCell(c);
            boolean isProcessoColumn = headerName.equalsIgnoreCase("Processo");

            if (isProcessoColumn) {
                // Processo é referência fixa da ETAPA 1 para manter consistência em todas as linhas.
                cell.setCellFormula(processoReferenceFormula);
            } else {
                Object val = rowData == null ? null : rowData.get(headerName);
                String text = val == null ? "" : String.valueOf(val);
                cell.setCellValue(normalizeSelectValue(headerName, text));
            }

            if (isProcessoColumn
                    || headerName.contains("Evento")
                    || headerName.contains("Causas")
                    || headerName.contains("Consequências")) {
                cell.setCellStyle(dataStyleLeft);
            } else {
                cell.setCellStyle(dataStyleCenter);
            }
        }
    }

    private void setColumnWidths(XSSFSheet sheet, List<String> headers) {
        applyColumnWidthsByHeaders(sheet, headers, header -> {
            if (header.contains("Evento") || header.contains("Causas")) {
                return 15000;
            }
            if (header.equalsIgnoreCase("Processo")) {
                return 12000;
            }
            if (header.contains("Consequências")) {
                return 40000;
            }
            return 7000;
        });
    }

    private String normalizeSelectValue(String headerName, String value) {
        if (value == null) {
            return "";
        }

        if (!"Tipo de Risco".equalsIgnoreCase(headerName)) {
            return value;
        }

        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("Ameaca") || trimmed.equalsIgnoreCase("Ameaça")) {
            return "Ameaça";
        }
        if (trimmed.equalsIgnoreCase("Oportunidade")) {
            return "Oportunidade";
        }
        return value;
    }

    private String buildProcessoReferenceFormula(String etapa1SheetName) {
        return "'" + etapa1SheetName.replace("'", "''") + "'!A$5";
    }

}
