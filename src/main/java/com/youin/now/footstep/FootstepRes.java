package com.youin.now.footstep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record FootstepRes(
        String id,
        String categoryId,
        String categoryName,
        String title,
        String who,
        String situation,
        String firstStep,
        JsonNode nextSteps,
        String quote
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static FootstepRes from(FootstepEntity e, String categoryName) {
        JsonNode steps;
        try {
            steps = MAPPER.readTree(e.getNextSteps());
        } catch (Exception ex) {
            steps = MAPPER.createArrayNode();
        }
        return new FootstepRes(
                e.getId(),
                e.getCategoryId(),
                categoryName,
                e.getTitle(),
                e.getWho(),
                e.getSituation(),
                e.getFirstStep(),
                steps,
                e.getQuote()
        );
    }
}