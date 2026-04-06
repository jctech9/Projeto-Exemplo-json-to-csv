package com.example.demo.transformers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolve referencias Jackson {@code @id} / {@code @ref} em uma arvore JSON
 * deserializada, substituindo stubs {@code {"@ref": "X"}} pelo objeto completo
 * correspondente.
 */
public final class RefResolver {

    private RefResolver() {
    }

    /**
     * Percorre a arvore JSON, coleta todos os objetos com {@code @id} e em
     * seguida substitui cada {@code {"@ref": "X"}} pelo objeto original compartilhado.
     * A operacao e feita in-place no mapa recebido.
     */
    public static void resolve(Map<String, Object> root) {
        if (root == null) return;

        // 1a passada: coletar registro @id -> objeto completo
        Map<String, Map<String, Object>> registry = new HashMap<>();
        collectIds(root, registry, Collections.newSetFromMap(new IdentityHashMap<>()));

        // 2a passada: substituir @ref stubs pelos objetos completos
        Set<String> unresolvedRefs = new LinkedHashSet<>();
        replaceRefs(root, registry, Collections.newSetFromMap(new IdentityHashMap<>()), unresolvedRefs);

        if (!unresolvedRefs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: referencias @ref sem objeto completo correspondente " + unresolvedRefs
            );
        }
    }

    /* ---------- coleta ---------- */

    @SuppressWarnings("unchecked")
    private static void collectIds(
            Object node,
            Map<String, Map<String, Object>> registry,
            Set<Object> visited
    ) {
        if (node == null || !visited.add(node)) return;

        if (node instanceof Map<?, ?> map) {
            Map<String, Object> m = (Map<String, Object>) map;
            Object atId = m.get("@id");
            if (atId != null && m.size() > 1) {
                String id = atId.toString();
                Map<String, Object> previous = registry.get(id);
                if (previous == null) {
                    registry.put(id, m);
                } else if (previous != m && shouldReplace(previous, m)) {
                    // Em caso de @id duplicado, prioriza o objeto mais completo
                    // para reduzir chance de manter estrutura parcial.
                    registry.put(id, m);
                }
            }
            for (Object value : m.values()) {
                collectIds(value, registry, visited);
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                collectIds(item, registry, visited);
            }
        }
    }

    /* ---------- substituicao ---------- */

    @SuppressWarnings("unchecked")
    private static void replaceRefs(
            Object node,
            Map<String, Map<String, Object>> registry,
            Set<Object> visited,
            Set<String> unresolvedRefs
    ) {
        if (node == null || !visited.add(node)) return; // evita ciclos

        if (node instanceof Map<?, ?> map) {
            Map<String, Object> m = (Map<String, Object>) map;
            // Itera sobre as chaves e substitui valores que sejam stubs @ref
            for (String key : new ArrayList<>(m.keySet())) {
                Object value = m.get(key);
                if (value instanceof Map<?, ?> child) {
                    Map<String, Object> childMap = (Map<String, Object>) child;
                    Object atRef = childMap.get("@ref");
                    if (atRef != null && childMap.size() == 1) {
                        // E um stub @ref - substituir pelo objeto completo
                        Map<String, Object> full = registry.get(atRef.toString());
                        if (full != null) {
                            m.put(key, full);
                        } else {
                            unresolvedRefs.add(atRef.toString());
                        }
                    } else {
                        replaceRefs(childMap, registry, visited, unresolvedRefs);
                    }
                } else if (value instanceof List<?>) {
                    replaceRefsInList((List<Object>) value, registry, visited, unresolvedRefs);
                }
            }
        } else if (node instanceof List<?>) {
            replaceRefsInList((List<Object>) node, registry, visited, unresolvedRefs);
        }
    }

    @SuppressWarnings("unchecked")
    private static void replaceRefsInList(
            List<Object> list,
            Map<String, Map<String, Object>> registry,
            Set<Object> visited,
            Set<String> unresolvedRefs
    ) {
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof Map<?, ?> child) {
                Map<String, Object> childMap = (Map<String, Object>) child;
                Object atRef = childMap.get("@ref");
                if (atRef != null && childMap.size() == 1) {
                    Map<String, Object> full = registry.get(atRef.toString());
                    if (full != null) {
                        list.set(i, full);
                    } else {
                        unresolvedRefs.add(atRef.toString());
                    }
                } else {
                    replaceRefs(childMap, registry, visited, unresolvedRefs);
                }
            } else if (item instanceof List<?>) {
                replaceRefsInList((List<Object>) item, registry, visited, unresolvedRefs);
            }
        }
    }

    private static boolean shouldReplace(Map<String, Object> current, Map<String, Object> candidate) {
        return infoScore(candidate) > infoScore(current);
    }

    private static int infoScore(Map<String, Object> map) {
        int score = 0;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = e.getKey();
            if ("@id".equals(key)) {
                continue;
            }

            Object value = e.getValue();
            if (value == null) {
                continue;
            }

            if (value instanceof Map<?, ?> child && child.size() == 1 && child.containsKey("@ref")) {
                continue;
            }
            score++;
        }
        return score;
    }
}
