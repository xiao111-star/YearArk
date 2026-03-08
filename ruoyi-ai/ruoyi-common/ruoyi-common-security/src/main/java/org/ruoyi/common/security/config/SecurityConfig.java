package org.ruoyi.common.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.satoken.utils.StpAnonUtil;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.ruoyi.common.security.config.properties.SecurityProperties;
import org.ruoyi.common.security.handler.AllUrlHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 权限安全配置
 *
 * @author Lion Li
 */

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;

    /**
     * 注册sa-token的拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 后台管理拦截器：使用 StpUtil 校验
        registry.addInterceptor(new SaInterceptor(handler -> {
                    AllUrlHandler allUrlHandler = SpringUtils.getBean(AllUrlHandler.class);
                    SaRouter.match(
                            "/api/user/album/**",
                            "/api/user/invite/**",
                            "/api/user/media/**",
                            "/api/user/template/**",
                            "/api/user/auth/logout",
                            "/api/user/auth/info"
                    ).check(StpUserUtil::checkLogin).stop();


                    SaRouter.match(
                            "/api/anonUser/invite/upload/**",
                            "/api/anonUser/invite/my-uploads"
                    ).check(StpAnonUtil::checkLogin).stop();


                    SaRouter
                            .match(allUrlHandler.getUrls())
                            .notMatch("/api/user/**", "/api/anonUser/**")
                            .check(() -> StpUtil.checkLogin());
                })).addPathPatterns("/**")
                .excludePathPatterns(securityProperties.getExcludes());
    }
}
