package com.youin.now.auth;

import com.youin.now.common.security.CurrentUserArgumentResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청에서 {@code Authorization: Bearer …} 를 읽어 <b>사용자 번호를 요청 속성에 넣습니다.</b>
 *
 * <p>그 뒤는 {@code common/security} 의 리졸버가 받아서, 컨트롤러가 이렇게만 쓰면 됩니다.
 *
 * <pre>
 * public ApiResponse&lt;TodayRes&gt; get(&#64;CurrentUser String userId) { ... }
 * </pre>
 *
 * <p><b>여기서 막지 않습니다.</b> 토큰이 없거나 틀려도 그냥 통과시키고 속성을 비워 둡니다.
 * 인증이 필요한 자리에서 리졸버가 {@code UNAUTHORIZED} 를 냅니다.
 * 이렇게 해야 {@code /auth/guest} 처럼 토큰 없이 부르는 API 를 예외 목록으로 관리하지 않아도 됩니다.
 */
@Component
@Order(1)
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final AuthService authService;

    public AuthTokenFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            String userId = authService.resolveUserId(header.substring(BEARER.length()).trim());
            if (userId != null) {
                request.setAttribute(CurrentUserArgumentResolver.ATTR, userId);
            }
        }
        chain.doFilter(request, response);
    }
}
