package com.youin.now.common.security;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUser} 가 붙은 파라미터에 {@code userId} 를 넣어 줍니다.
 *
 * <p>값은 인증 필터가 요청 속성 {@link #ATTR} 에 넣어 둡니다.
 * <b>필터 구현은 이철희 님의 {@code auth/} 입니다</b> — 인증 방식(JWT · 세션 · 매직링크)이 정해진 뒤에 붙습니다.
 * 그때까지는 필터가 없으므로 {@code UNAUTHORIZED} 가 납니다.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /** 인증 필터가 여기에 {@code Long userId} 를 넣습니다. */
    public static final String ATTR = "now.userId";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mav,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Object v = webRequest.getAttribute(ATTR, RequestAttributes.SCOPE_REQUEST);
        if (v instanceof Long userId) return userId;
        throw new ApiException(ErrorCode.UNAUTHORIZED);
    }
}
