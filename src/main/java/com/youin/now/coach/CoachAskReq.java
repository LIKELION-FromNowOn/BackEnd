package com.youin.now.coach;

import jakarta.validation.constraints.Size;

/**
 * {@code NOW-COACH-001} 요청.
 *
 * <p><b>명세는 {@code question} 인데 프론트 목은 {@code text} 를 보냅니다.</b>
 * 둘 다 받습니다 — 어느 쪽으로 와도 동작합니다. {@code question} 이 정본입니다.
 */
public record CoachAskReq(@Size(max = 500, message = "질문이 너무 깁니다") String question,
                          @Size(max = 500, message = "질문이 너무 깁니다") String text) {

    /** 둘 중 채워진 것. 둘 다 비면 {@code null} */
    public String ask() {
        if (question != null && !question.isBlank()) return question;
        if (text != null && !text.isBlank()) return text;
        return null;
    }
}
