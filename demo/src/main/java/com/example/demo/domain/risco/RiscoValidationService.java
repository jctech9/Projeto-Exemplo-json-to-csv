package com.example.demo.domain.risco;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
// Regras de consistencia para colecoes que referenciam risco.
public class RiscoValidationService {

    private final RiscoAlignmentService riscoAlignmentService;

    public RiscoValidationService(RiscoAlignmentService riscoAlignmentService) {
        this.riscoAlignmentService = riscoAlignmentService;
    }

    public Set<Integer> buildCanonicalRiscoIds(
            List<Map<String, Object>> riscosContent,
            List<Map<String, Object>> avaliacoesContent,
            List<Map<String, Object>> respostasContent,
            List<Map<String, Object>> atividadesContent,
            List<Map<String, Object>> ocorrenciasContent
    ) {
        if (riscosContent != null && !riscosContent.isEmpty()) {
            return extractCanonicalRiscoIdsFromRiscos(riscosContent);
        }

        Set<Integer> inferred = new LinkedHashSet<>();
        appendRiscoIds(inferred, avaliacoesContent);
        appendRiscoIds(inferred, respostasContent);
        appendRiscoIds(inferred, atividadesContent);
        appendRiscoIds(inferred, ocorrenciasContent);
        return inferred;
    }

    public int countNonEmptyCollections(List<Map<String, Object>>... collections) {
        int count = 0;
        for (List<Map<String, Object>> collection : collections) {
            if (collection != null && !collection.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public void validateStrictRiscoCollection(
            String collectionName,
            List<Map<String, Object>> content,
            Set<Integer> canonicalRiscoIds
    ) {
        // Validacao estrita evita desalinhar as etapas por risco.id.
        if (content == null || content.isEmpty() || canonicalRiscoIds == null || canonicalRiscoIds.isEmpty()) {
            return;
        }

        List<Integer> missingRiscoIdPositions = new ArrayList<>();
        Set<Integer> outOfCanonical = new LinkedHashSet<>();
        Set<Integer> duplicated = new LinkedHashSet<>();
        Set<Integer> seen = new LinkedHashSet<>();

        for (int i = 0; i < content.size(); i++) {
            Integer riscoId = riscoAlignmentService.extractRiscoId(content.get(i));
            if (riscoId == null) {
                missingRiscoIdPositions.add(i + 1);
                continue;
            }

            if (!canonicalRiscoIds.contains(riscoId)) {
                outOfCanonical.add(riscoId);
            }

            if (!seen.add(riscoId)) {
                duplicated.add(riscoId);
            }
        }

        if (!missingRiscoIdPositions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao '" + collectionName
                            + "' possui itens sem risco.id nas posicoes " + missingRiscoIdPositions + "."
            );
        }

        if (!outOfCanonical.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao '" + collectionName
                            + "' possui risco.id fora da lista canonica " + outOfCanonical + "."
            );
        }

        if (!duplicated.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao '" + collectionName
                            + "' possui risco.id duplicado " + duplicated + "."
            );
        }
    }

    private Set<Integer> extractCanonicalRiscoIdsFromRiscos(List<Map<String, Object>> riscosContent) {
        Set<Integer> canonical = new LinkedHashSet<>();
        Set<Integer> duplicated = new LinkedHashSet<>();
        List<Integer> missingPositions = new ArrayList<>();

        for (int i = 0; i < riscosContent.size(); i++) {
            Integer riscoId = extractEntityId(riscosContent.get(i));
            if (riscoId == null) {
                missingPositions.add(i + 1);
                continue;
            }

            if (!canonical.add(riscoId)) {
                duplicated.add(riscoId);
            }
        }

        if (!missingPositions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao 'riscos' possui itens sem id nas posicoes " + missingPositions + "."
            );
        }

        if (!duplicated.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao 'riscos' possui ids duplicados " + duplicated + "."
            );
        }

        return canonical;
    }

    private void appendRiscoIds(Set<Integer> target, List<Map<String, Object>> content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        for (Map<String, Object> item : content) {
            Integer riscoId = riscoAlignmentService.extractRiscoId(item);
            if (riscoId != null) {
                target.add(riscoId);
            }
        }
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
