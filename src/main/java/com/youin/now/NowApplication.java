package com.youin.now;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 지금부터 (now) — 백엔드 진입점.
 *
 * <p>패키지 14개가 이 아래에 있고 <b>폴더 하나에 주인이 한 명</b>입니다.
 * 누가 어느 폴더를 쓰는지는 {@code docs/03-packages.md} 를 보십시오.
 *
 * <p>클래스 이름이 겹치면 Git 충돌이 아니라 <b>스프링 부팅이 실패</b>합니다
 * ({@code ConflictingBeanDefinitionException}). 그래서 클래스 이름에 패키지 접두어를 붙입니다.
 */
@SpringBootApplication
public class NowApplication {

    public static void main(String[] args) {
        SpringApplication.run(NowApplication.class, args);
    }
}
