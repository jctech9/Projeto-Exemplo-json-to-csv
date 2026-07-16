package com.example.demo.web;

import com.example.demo.application.export.ExcelService;
import com.example.demo.application.export.ExportSheetBuilderService;
import com.example.demo.application.export.input.api.ApiDataFetchException;
import com.example.demo.config.InvalidApiDestinationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/export")
public class ExcelController {

    private static final Logger log = LoggerFactory.getLogger(ExcelController.class);

    private final ExcelService excelService;
    private final ExportSheetBuilderService exportSheetBuilderService;

    public ExcelController(ExcelService excelService, ExportSheetBuilderService exportSheetBuilderService) {
        this.excelService = excelService;
        this.exportSheetBuilderService = exportSheetBuilderService;
    }

    @GetMapping("/xlsx/{id}")
    public ResponseEntity<byte[]> exportByProcessId(
            @PathVariable("id") int id,
            @RequestParam(value = "baseUrl", required = false) String rejectedBaseUrl
    ) throws IOException {
        if (rejectedBaseUrl != null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Parametro de destino nao e aceito.".getBytes(StandardCharsets.UTF_8));
        }

        Map<String, List<Map<String, Object>>> allSheets;

        try {
            allSheets = exportSheetBuilderService.buildSheetsFromProcessId(id);
        } catch (InvalidApiDestinationException e) {
            log.error("event=api_export_invalid_server_destination process_id={}", id);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Integracao com API origem indisponivel.".getBytes(StandardCharsets.UTF_8));
        } catch (ApiDataFetchException e) {
            log.error(
                "event=api_export_failed policy=fail_fast endpoint={} process_id={}",
                e.getEndpoint(),
                id
            );
            String message = "Falha de integracao com API origem. Nenhum arquivo parcial foi gerado.";
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.TEXT_PLAIN)
                .body(message.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            log.error(
                    "event=api_export_invalid_data policy=fail_fast process_id={}",
                    id
            );
            String message = "Falha de consistencia nos dados da API origem. Nenhum arquivo parcial foi gerado.";
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(message.getBytes(StandardCharsets.UTF_8));
        }

        return buildExcelResponse(allSheets);
    }

    private ResponseEntity<byte[]> buildExcelResponse(Map<String, List<Map<String, Object>>> allSheets) throws IOException {
        if (allSheets.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        byte[] bytes = excelService.generateXlsx(allSheets);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dados.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
