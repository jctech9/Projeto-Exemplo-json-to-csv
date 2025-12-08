package com.example.demo;

import java.util.*;

public class JsonDataTransformer {

    /**
     * Transforma JSON em formato compatível com o Excel export.
     * Detecta automaticamente se é paginado e adapta.
     */
    public static Map<String, List<Map<String, Object>>> transform(Map<String, Object> input) {
        // Se detectar resposta paginada (tem "content" e "pageable")
        if (input.containsKey("content") && input.containsKey("pageable")) {
            return transformPaginatedResponse(input);
        }

        // Se for um array simples, envolve em uma aba "Dados"
        if (input.containsKey("data") || input.values().stream().anyMatch(v -> v instanceof List)) {
            return transformSimpleArrays(input);
        }

        // Se já estiver no formato correto (Map de abas com arrays)
        if (isValidExcelFormat(input)) {
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> formatted = 
                (Map<String, List<Map<String, Object>>>) (Map<String, ?>) input;
            return formatted;
        }

        // Fallback: cria uma aba genérica
        return wrapInDefaultSheet(input);
    }

    /**
     * Transforma resposta paginada do Spring Data (como do Gestão de Riscos)
     */
    private static Map<String, List<Map<String, Object>>> transformPaginatedResponse(Map<String, Object> input) {
        List<?> content = (List<?>) input.get("content");
        String sheetName = detectSheetName(input);

        List<Map<String, Object>> rows = new ArrayList<>();

        if (content != null) {
            for (Object item : content) {
                if (item instanceof Map) {
                    Map<String, Object> flattenedRow = flattenObject((Map<String, Object>) item, "");
                    rows.add(flattenedRow);
                }
            }
        }

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put(sheetName, rows);
        return result;
    }

    /**
     * Detecta o nome da aba baseado no conteúdo
     */
    private static String detectSheetName(Map<String, Object> input) {
        List<?> content = (List<?>) input.get("content");
        if (content != null && !content.isEmpty()) {
            Object first = content.get(0);
            if (first instanceof Map) {
                Map<String, Object> firstItem = (Map<String, Object>) first;
                // Detecta tipo pela presença de campos específicos
                if (firstItem.containsKey("processo")) return "Processos";
                if (firstItem.containsKey("acao")) return "Ações";
                if (firstItem.containsKey("risco")) return "Riscos";
                if (firstItem.containsKey("mitigacao")) return "Mitigações";
                if (firstItem.containsKey("nome") && firstItem.containsKey("unidadeOrganizacional")) return "Processos";
            }
        }
        return "Dados";
    }

    /**
     * Achata objetos aninhados, convertendo para strings legíveis
     */
    private static Map<String, Object> flattenObject(Map<String, Object> obj, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Ignora campos desnecessários
            if (key.startsWith("@") || key.equals("pageable") || key.equals("sort")) {
                continue;
            }

            String columnName = formatColumnName(key);

            if (value == null) {
                result.put(columnName, "");
            } else if (value instanceof Map) {
                // Para objetos aninhados, extrai campos principais
                Map<String, Object> nested = (Map<String, Object>) value;
                String flattened = flattenNestedObject(nested, key);
                if (!flattened.isEmpty()) {
                    result.put(columnName, flattened);
                }
            } else if (value instanceof List) {
                // Para listas, ignora ou converte para count
                result.put(columnName, "");
            } else {
                result.put(columnName, value.toString());
            }
        }

        return result;
    }

    /**
     * Achata objeto aninhado extraindo os campos mais relevantes
     */
    private static String flattenNestedObject(Map<String, Object> nested, String parentKey) {
        List<String> parts = new ArrayList<>();

        // Prioriza certos campos
        String[] priorityFields = {"nome", "sigla", "email", "papel", "titulo"};
        
        for (String field : priorityFields) {
            if (nested.containsKey(field)) {
                parts.add(nested.get(field).toString());
            }
        }

        return String.join(" | ", parts);
    }

    /**
     * Formata nome da coluna (camelCase -> Title Case)
     */
    private static String formatColumnName(String key) {
        // Remove prefixos em português
        key = key.replaceAll("^(id|unidade|responsavel|objetivo)Organisacional$", "$1");
        
        // camelCase -> Title Case
        key = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        // Maiúscula na primeira letra
        return key.substring(0, 1).toUpperCase() + key.substring(1);
    }

    /**
     * Verifica se já está no formato correto: Map com chaves string e valores são List<Map>
     */
    private static boolean isValidExcelFormat(Map<String, Object> input) {
        if (input.isEmpty()) {
            return false;
        }
        for (Object value : input.values()) {
            if (!(value instanceof List)) {
                return false;
            }
            List<?> list = (List<?>) value;
            if (!list.isEmpty() && !(list.get(0) instanceof Map)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Transforma arrays simples
     */
    private static Map<String, List<Map<String, Object>>> transformSimpleArrays(Map<String, Object> input) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (entry.getValue() instanceof List) {
                List<Map<String, Object>> rows = new ArrayList<>();
                List<?> list = (List<?>) entry.getValue();

                for (Object item : list) {
                    if (item instanceof Map) {
                        rows.add((Map<String, Object>) item);
                    }
                }

                if (!rows.isEmpty()) {
                    result.put(entry.getKey(), rows);
                }
            }
        }

        return result;
    }

    /**
     * Fallback: envolve em aba padrão
     */
    private static Map<String, List<Map<String, Object>>> wrapInDefaultSheet(Map<String, Object> input) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(flattenObject(input, ""));
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("Dados", rows);
        return result;
    }
}
