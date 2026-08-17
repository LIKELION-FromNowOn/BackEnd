package com.youin.now.common.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 리졸버 등록과 CORS.
 *
 * <p><b>CORS 에 프론트 배포 주소를 넣지 않으면 화면에서 API 가 전부 실패합니다.</b>
 * 주소는 코드에 박지 않고 {@code app.cors.origins} 로 뺐습니다 — 배포 주소가 바뀔 때 재배포만 하면 됩니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserResolver;

    /** 쉼표로 여러 개. 예 — {@code http://localhost:5173,https://xxx.vercel.app} */
    @Value("${app.cors.origins:http://localhost:5173}")
    private String[] origins;

    public WebConfig(CurrentUserArgumentResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 경로는 context-path(/api/v1) 를 뺀 뒤의 값과 맞춰집니다.
        // 여기에 "/api/**" 를 쓰면 실제 경로가 "/subtract/evaluate" 라 하나도 안 걸립니다
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
