package com.example.demo.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TransformerUtils {
    private TransformerUtils() {
        // utilitário, não instanciar
    }

    public static String val(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object v) {
        return v instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    /** Retorna a lista content já filtrada para Map. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getContent(Map<String, Object> input) {
        Object contentObj = input.get("content");
        if (!(contentObj instanceof List<?> list)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) out.add((Map<String, Object>) m);
        }
        return out;
    }

    /** Navegação segura em mapas aninhados. */
    @SuppressWarnings("unchecked")
    public static String getNestedString(Map<String, Object> map, String... path) {
        Object curr = map;
        for (String key : path) {
            if (!(curr instanceof Map<?, ?> m)) return "";
            curr = m.get(key);
        }
        return val(curr);
    }
}
