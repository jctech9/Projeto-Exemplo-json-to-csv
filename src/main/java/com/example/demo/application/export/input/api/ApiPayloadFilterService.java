package com.example.demo.application.export.input.api;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
// Normaliza e filtra payloads por processo e por id, incluindo suporte a @id/@ref.
public class ApiPayloadFilterService {

    public List<Map<String, Object>> filterByExactId(Map<String, Object> data, int id) {
        List<Map<String, Object>> content = getContentList(data);
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        Map<String, Integer> refCache = new HashMap<>();
        for (Map<String, Object> item : content) {
            if (checkId(item, id, refCache)) {
                filtered.add(item);
                break;
            }
        }

        return filtered;
    }

    public List<Map<String, Object>> filterByProcess(Map<String, Object> data, int processId) {
        List<Map<String, Object>> content = getContentList(data);
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        Map<String, Integer> refCache = new HashMap<>();
        for (Map<String, Object> item : content) {
            if (isRelatedToProcess(item, processId, refCache)) {
                filtered.add(item);
            }
        }

        return filtered;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getContentList(Map<String, Object> data) {
        if (data != null && data.containsKey("content")) {
            return (List<Map<String, Object>>) data.get("content");
        }
        return null;
    }

    public Integer parseIntegerId(Object rawId) {
        if (rawId == null) {
            return null;
        }
        if (rawId instanceof Number number) {
            return number.intValue();
        }

        String value = String.valueOf(rawId).trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isRelatedToProcess(Map<String, Object> item, int processId, Map<String, Integer> refCache) {
        checkId(item, -1, refCache);

        Object procObj = item.get("processo");
        if (procObj instanceof Map<?, ?> procMap) {
            if (checkId((Map<String, Object>) procMap, processId, refCache)) {
                return true;
            }
        }

        Object riscoObj = item.get("risco");
        if (riscoObj instanceof Map<?, ?> riscoMap) {
            Map<String, Object> risco = (Map<String, Object>) riscoMap;
            checkId(risco, -1, refCache);

            Object procViaRisco = risco.get("processo");
            if (procViaRisco instanceof Map<?, ?> procMap) {
                if (checkId((Map<String, Object>) procMap, processId, refCache)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkId(Map<String, Object> obj, int processId, Map<String, Integer> refCache) {
        if (obj == null) {
            return false;
        }

        Object idVal = obj.get("id");
        Object atId = obj.get("@id");
        Object atRef = obj.get("@ref");

        Integer currentId = parseIntegerId(idVal);
        if (currentId != null) {
            if (atId != null) {
                refCache.put(atId.toString(), currentId);
            }
            return currentId == processId;
        }

        if (atRef != null && refCache.containsKey(atRef.toString())) {
            int cachedId = refCache.get(atRef.toString());
            return cachedId == processId;
        }

        return false;
    }
}
