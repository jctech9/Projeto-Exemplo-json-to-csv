package com.example.demo;

import com.example.demo.service.ExcelService;
import com.example.demo.service.ExportSheetBuilderService;
import org.springframework.http.HttpHeaders;
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
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(("Erro ao buscar dados da API: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
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
