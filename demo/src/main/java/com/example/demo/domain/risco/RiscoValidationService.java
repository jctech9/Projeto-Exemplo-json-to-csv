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
}
