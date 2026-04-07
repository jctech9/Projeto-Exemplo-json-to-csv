package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
// Fachada de orquestracao: delega por origem dos dados (payload ou API).
public class ExportSheetBuilderService {

    private final PayloadSheetBuilder payloadSheetBuilder;
    private final ApiSheetBuilder apiSheetBuilder;

    public ExportSheetBuilderService(PayloadSheetBuilder payloadSheetBuilder, ApiSheetBuilder apiSheetBuilder) {
        this.payloadSheetBuilder = payloadSheetBuilder;
        this.apiSheetBuilder = apiSheetBuilder;
    }

    public Map<String, List<Map<String, Object>>> buildSheetsFromPayload(Map<String, Object> payload) {
        return payloadSheetBuilder.buildSheetsFromPayload(payload);
    }

    public Map<String, List<Map<String, Object>>> buildSheetsFromApi(String baseUrl, Map<String, Integer> body) {
        return apiSheetBuilder.buildSheetsFromApi(baseUrl, body);
    }
}
