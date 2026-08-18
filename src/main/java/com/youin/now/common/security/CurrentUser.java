package com.youin.now.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러에서 로그인한 사용자 번호를 받는 표시.
 *
 * <pre>
 * public ApiResponse&lt;TodayRes&gt; get(&#64;CurrentUser String userId) { ... }
 * </pre>
 *
 * <p><b>{@code AuthUser} 엔티티를 넘기지 않고 사용자 번호({@code String})만 넘깁니다.</b>
 * {@code @ManyToOne AuthUser} 를 쓰면 패키지 14개가 한 덩어리가 되고, 셋이 같은 파일을 매일 건드립니다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
