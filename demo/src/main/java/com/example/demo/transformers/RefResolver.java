package com.example.demo.transformers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * Resolve referências Jackson {@code @id} / {@code @ref} em uma árvore JSON
 * deserializada, substituindo stubs {@code {"@ref": "X"}} pelo objeto completo
 * correspondente.  Deve ser chamado <b>antes</b> dos transformers.
 */
public final class RefResolver {

    private RefResolver() { }

    /**
     * Percorre a árvore JSON, coleta todos os objetos com {@code @id} e em
     * seguida substitui cada {@code {"@ref": "X"}} pela cópia do objeto original.
     * A operação é feita in-place no mapa recebido.
     */
    @SuppressWarnings("unchecked")
    public static void resolve(Map<String, Object> root) {
        if (root == null) return;

        // 1ª passada: coletar registro @id → objeto completo
        Map<String, Map<String, Object>> registry = new HashMap<>();
        collectIds(root, registry);

        // 2ª passada: substituir @ref stubs pelos objetos completos
        replaceRefs(root, registry, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /* ---------- coleta ---------- */

    @SuppressWarnings("unchecked")
    private static void collectIds(Object node, Map<String, Map<String, Object>> registry) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> m = (Map<String, Object>) map;
            Object atId = m.get("@id");
            if (atId != null && m.size() > 1) {
                // Objeto completo (tem @id + outros campos)
                registry.put(atId.toString(), m);
            }
            for (Object value : m.values()) {
                collectIds(value, registry);
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                collectIds(item, registry);
            }
        }
    }

    /* ---------- substituição ---------- */

    @SuppressWarnings("unchecked")
    private static void replaceRefs(Object node, Map<String, Map<String, Object>> registry,
                                     Set<Object> visited) {
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
                        // É um stub @ref — substituir pelo objeto completo
                        Map<String, Object> full = registry.get(atRef.toString());
                        if (full != null) {
                            m.put(key, full);
                        }
                    } else {
                        replaceRefs(childMap, registry, visited);
                    }
                } else if (value instanceof List<?>) {
                    replaceRefsInList((List<Object>) value, registry, visited);
                }
            }
        } else if (node instanceof List<?>) {
            replaceRefsInList((List<Object>) node, registry, visited);
        }
    }

    @SuppressWarnings("unchecked")
    private static void replaceRefsInList(List<Object> list, Map<String, Map<String, Object>> registry,
                                           Set<Object> visited) {
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof Map<?, ?> child) {
                Map<String, Object> childMap = (Map<String, Object>) child;
                Object atRef = childMap.get("@ref");
                if (atRef != null && childMap.size() == 1) {
                    Map<String, Object> full = registry.get(atRef.toString());
                    if (full != null) {
                        list.set(i, full);
                    }
                } else {
                    replaceRefs(childMap, registry, visited);
                }
            } else if (item instanceof List<?>) {
                replaceRefsInList((List<Object>) item, registry, visited);
            }
        }
    }
}
