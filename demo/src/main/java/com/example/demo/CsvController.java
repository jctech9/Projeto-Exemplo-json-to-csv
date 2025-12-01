package com.example.demo;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class CsvController {

    private final CsvService csvService;

    @PostMapping("/csv")
    public ResponseEntity<byte[]> exportToCsv(@RequestBody List<Map<String, Object>> data) {
        byte[] csvBytes = csvService.generateCsv(data);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dados.csv");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(csvBytes.length)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}