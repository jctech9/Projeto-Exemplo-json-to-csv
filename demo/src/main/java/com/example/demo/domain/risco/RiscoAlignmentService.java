package com.example.demo.domain.risco;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
// Centraliza alinhamento e normalizacao de dados por risco.id.
public class RiscoAlignmentService {

    public List<Map<String, Object>> alignByRiscoIds(List<Map<String, Object>> content, Set<Integer> canonicalRiscoIds) {
        if (canonicalRiscoIds == null || canonicalRiscoIds.isEmpty()) {
            if (content == null || content.isEmpty()) {
                return new ArrayList<>();
            }
            return new ArrayList<>(content);
        }

        if (content == null || content.isEmpty()) {
            List<Map<String, Object>> onlyPlaceholders = new ArrayList<>();
            for (Integer riscoId : canonicalRiscoIds) {
                onlyPlaceholders.add(createPlaceholderByRiscoId(riscoId));
            }
            return onlyPlaceholders;
        }

        Map<Integer, List<Map<String, Object>>> byRiscoId = new LinkedHashMap<>();
        List<Map<String, Object>> withoutRiscoId = new ArrayList<>();

        for (Map<String, Object> item : content) {
            Integer riscoId = extractRiscoId(item);
            if (riscoId == null) {
                withoutRiscoId.add(item);
                continue;
            }
            byRiscoId.computeIfAbsent(riscoId, key -> new ArrayList<>()).add(item);
        }

        List<Map<String, Object>> aligned = new ArrayList<>();
        Set<Integer> includedRiscoIds = new LinkedHashSet<>();
        for (Integer riscoId : canonicalRiscoIds) {
            List<Map<String, Object>> items = byRiscoId.get(riscoId);
            if (items != null && !items.isEmpty()) {
                aligned.addAll(items);
                includedRiscoIds.add(riscoId);
            } else {
                aligned.add(createPlaceholderByRiscoId(riscoId));
            }
        }

        for (Map.Entry<Integer, List<Map<String, Object>>> entry : byRiscoId.entrySet()) {
            if (!includedRiscoIds.contains(entry.getKey())) {
                aligned.addAll(entry.getValue());
            }
        }

        aligned.addAll(withoutRiscoId);
        return aligned;
    }

    public Map<Integer, String> buildRiscoNomePorId(List<Map<String, Object>> riscosContent) {
        Map<Integer, String> riscoNomePorId = new LinkedHashMap<>();
        if (riscosContent == null || riscosContent.isEmpty()) {
            return riscoNomePorId;
        }

        for (Map<String, Object> risco : riscosContent) {
            Integer riscoId = extractEntityId(risco);
            if (riscoId == null) {
                continue;
            }
            riscoNomePorId.put(riscoId, String.valueOf(risco.getOrDefault("nome", "")));
        }
        return riscoNomePorId;
    }

    @SuppressWarnings("unchecked")
    public void applyCanonicalRiscoNome(List<Map<String, Object>> items, Map<Integer, String> riscoNomePorId) {
        if (items == null || items.isEmpty() || riscoNomePorId == null || riscoNomePorId.isEmpty()) {
            return;
        }

        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }

            Integer riscoId = extractRiscoId(item);
            if (riscoId == null) {
                continue;
            }

            String nomeCanonico = riscoNomePorId.getOrDefault(riscoId, "");
            if (nomeCanonico.isBlank()) {
                continue;
            }

            Object riscoObj = item.get("risco");
            Map<String, Object> risco;
            if (riscoObj instanceof Map<?, ?> riscoMap) {
                risco = (Map<String, Object>) riscoMap;
            } else {
                risco = new LinkedHashMap<>();
                risco.put("id", riscoId);
                item.put("risco", risco);
            }
            risco.put("nome", nomeCanonico);
        }
    }

    private Map<String, Object> createPlaceholderByRiscoId(Integer riscoId) {
        Map<String, Object> placeholder = new LinkedHashMap<>();
        Map<String, Object> risco = new LinkedHashMap<>();
        risco.put("id", riscoId);
        placeholder.put("risco", risco);
        return placeholder;
    }

    public Integer extractRiscoId(Map<String, Object> item) {
        if (item == null) {
            return null;
        }

        Object riscoObj = item.get("risco");
        if (riscoObj instanceof Map<?, ?> riscoMap) {
            Object nestedId = riscoMap.get("id");
            if (nestedId instanceof Number) {
                return ((Number) nestedId).intValue();
            }
        }

        return null;
    }

    private Integer extractEntityId(Map<String, Object> item) {
        if (item == null) {
            return null;
        }
        Object id = item.get("id");
        if (id instanceof Number) {
            return ((Number) id).intValue();
        }
        return null;
    }
}
