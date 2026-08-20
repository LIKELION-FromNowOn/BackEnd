package com.youin.now.footstep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record FootstepRes(
        String id,
        String categoryId,
        String title,
        String who,
        String situation,
        String firstStep,
        JsonNode nextSteps,
        String quote,
        Boolean isOnboarding
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static FootstepRes from(FootstepEntity e) {
        JsonNode steps;
        try {
            steps = MAPPER.readTree(e.getNextSteps());
        } catch (Exception ex) {
            steps = MAPPER.createArrayNode();
        }
        return new FootstepRes(
                e.getId(),
                e.getCategoryId(),
                e.getTitle(),
                e.getWho(),
                e.getSituation(),
                e.getFirstStep(),
                steps,
                e.getQuote(),
                e.getIsOnboarding()
        );
    }
}