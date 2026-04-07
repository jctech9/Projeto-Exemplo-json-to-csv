package com.example.demo.web;

import com.example.demo.application.export.ExcelService;
import com.example.demo.application.export.ExportSheetBuilderService;
import com.example.demo.application.export.input.api.ApiDataFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

    @PostMapping("/xlsx")
    public ResponseEntity<byte[]> exportToXlsx(@RequestBody Map<String, Object> payload) throws IOException {
        Map<String, List<Map<String, Object>>> allSheets;

        try {
            allSheets = exportSheetBuilderService.buildSheetsFromPayload(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        }

        return buildExcelResponse(allSheets);
    }

    @GetMapping("/xlsx/api")
    public ResponseEntity<byte[]> exportFromApiGet(
            @RequestParam(value = "baseUrl", defaultValue = "http://localhost:8090") String baseUrl,
            @RequestParam(value = "id", required = false) Integer id
    ) throws IOException {
        Map<String, Integer> body = null;
        if (id != null) {
            body = new HashMap<>();
            body.put("id", id);
        }
        return exportFromApi(baseUrl, body);
    }

    @PostMapping("/xlsx/api")
    public ResponseEntity<byte[]> exportFromApi(
            @RequestParam(value = "baseUrl", defaultValue = "http://localhost:8090") String baseUrl,
            @RequestBody(required = false) Map<String, Integer> body
    ) throws IOException {
        Map<String, List<Map<String, Object>>> allSheets;

        try {
            allSheets = exportSheetBuilderService.buildSheetsFromApi(baseUrl, body);
        } catch (ApiDataFetchException e) {
            log.error(
                "event=api_export_failed policy=fail_fast endpoint={} url={} message={}",
                e.getEndpoint(),
                e.getUrl(),
                e.getMessage(),
                e
            );
            String message = "Falha de integracao com API origem (politica fail-fast). Nenhum arquivo parcial foi gerado. "
                + e.getMessage();
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
