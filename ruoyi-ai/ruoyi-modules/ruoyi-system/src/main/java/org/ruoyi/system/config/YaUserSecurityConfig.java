package org.ruoyi.system.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import org.ruoyi.system.util.StpAnonUtil;
import org.ruoyi.system.util.StpUserUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户端 Sa-Token 路由拦截配置
 * <p>
 * 拦截 /api/user/** 路径，按接口类型使用不同的认证体系：
 * - 已登录用户接口：StpUserUtil（Ya-Auth）
 * - 匿名上传接口：StpAnonUtil（Ya-Anon-Auth）
 * - 公开接口：不拦截
 */
@Configuration
public class YaUserSecurityConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SaInterceptor interceptor = new SaInterceptor(handler -> {
            // 已登录用户接口：使用 StpUserUtil 校验
            SaRouter.match(
                "/api/user/album/**",
                "/api/user/invite/**",
                "/api/user/media/**",
                "/api/user/template/**"
            ).check(StpUserUtil::checkLogin);

            // 匿名上传接口：使用 StpAnonUtil 校验
            SaRouter.match(
                "/api/user/share/upload/**",
                "/api/user/share/my-uploads"
            ).check(StpAnonUtil::checkLogin);

            // 公开接口（不拦截）：
            // /api/user/auth/**              → 注册登录
            // /api/user/share/{code}         → 验证邀请码
            // /api/user/share/{code}/verify  → 验证访问码
        });
        // 关闭注解鉴权，避免 SaInterceptor 默认执行 StpUtil.checkLogin()
        interceptor.isAnnotation = false;

        registry.addInterceptor(interceptor).addPathPatterns("/api/user/**");
    }
}
