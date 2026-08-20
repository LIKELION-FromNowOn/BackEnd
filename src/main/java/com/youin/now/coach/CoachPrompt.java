package com.youin.now.coach;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 시스템 프롬프트를 읽어 둡니다. <b>기동할 때 한 번만</b> 읽습니다.
 *
 * <p>파일은 {@code src/main/resources/prompts/coach-answer.txt} 이고,
 * <b>{@code docs/prompts/04-coach-answer.md} 에서 뽑아낸 것</b>입니다.
 * 손으로 옮기지 않았습니다 — {@code db/tools/sync_prompts.py} 가 뽑습니다.
 *
 * <p><b>프롬프트를 고치면 문서를 고치고 그 스크립트를 다시 돌리십시오.</b>
 * 코드에 직접 적으면 문서와 갈라지고, 갈라진 것을 아무도 눈치채지 못합니다.
 *
 * <p>없으면 <b>기동할 때 바로 실패합니다.</b> 조용히 빈 프롬프트로 LLM 을 부르면
 * 가드레일이 전부 사라진 채로 답이 나갑니다.
 */
@Component
public class CoachPrompt {

    private final String system;

    public CoachPrompt() {
        this.system = read("prompts/coach-answer.txt");
    }

    public String system() { return system; }

    private static String read(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            String s = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            if (s.isEmpty()) throw new IllegalStateException("프롬프트가 비어 있습니다: " + path);
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "프롬프트를 못 읽었습니다: " + path
                    + " — db/tools/sync_prompts.py 를 돌려 주십시오", e);
        }
    }
}
