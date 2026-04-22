package com.example.demo.application.export.input.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ApiRiscoContext(
        Map<String, List<Map<String, Object>>> sheets,
        Set<Integer> riscoIdsDoProcesso,
        Map<Integer, String> riscoNomePorId
) {
}
