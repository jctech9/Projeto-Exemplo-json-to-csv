package com.example.demo.application.export;

import com.example.demo.application.export.input.api.ApiSheetBuilder;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
// Fachada de orquestracao: exportacao orientada a processo (id).
public class ExportSheetBuilderService {

    private final ApiSheetBuilder apiSheetBuilder;

    public ExportSheetBuilderService(ApiSheetBuilder apiSheetBuilder) {
        this.apiSheetBuilder = apiSheetBuilder;
    }

    public Map<String, List<Map<String, Object>>> buildSheetsFromProcessId(String baseUrl, int processId) {
        return apiSheetBuilder.buildSheetsFromApi(baseUrl, processId);
    }
}
