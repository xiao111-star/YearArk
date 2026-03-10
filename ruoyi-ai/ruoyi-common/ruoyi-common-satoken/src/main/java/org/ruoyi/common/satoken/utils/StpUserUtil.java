package org.ruoyi.common.satoken.utils;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;

/**
 * 用户端已登录用户的 Sa-Token 操作工具类
 * <p>
 * 对应 ya_user 表，使用独立的 StpLogic 实例，token-name: Ya-Auth
 * 与管理端 StpUtil（Authorization）和匿名端 StpAnonUtil（Ya-Anon-Auth）互不干扰
 */
public class StpUserUtil {

    public static final String TYPE = "ya-user";

    public static final StpLogic stpLogic = new StpLogicJwtForSimple(TYPE) {
        @Override
        public String splicingKeyTokenName() {
            return "Ya-Auth";
        }
    };

    private StpUserUtil() {
    }

    /**
     * 登录
     */
    public static void login(Object id) {
        stpLogic.login(id);
    }

    /**
     * 注销
     */
    public static void logout() {
        stpLogic.logout();
    }

    /**
     * 检查当前是否已登录，未登录则抛出异常
     */
    public static void checkLogin() {
        stpLogic.checkLogin();
    }

    /**
     * 获取当前登录用户的 loginId，转为 int
     */
    public static int getLoginIdAsInt() {
        return stpLogic.getLoginIdAsInt();
    }

    /**
     * 获取当前 token 值
     */
    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }

    /**
     * 获取当前 token 对应的 session
     */
    public static SaSession getTokenSession() {
        return stpLogic.getTokenSession();
    }

    /**
     * 判断当前是否已登录
     */
    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    /**
     * 获取当前登录用户的 loginId
     */
    public static Object getLoginId() {
        return stpLogic.getLoginId();
    }

    /**
     * 根据 token 值获取对应的 session
     */
    public static SaSession getTokenSessionByToken(String tokenValue) {
        return stpLogic.getTokenSessionByToken(tokenValue);
    }

    /**
     * 根据 token 值注销
     */
    public static void logoutByTokenValue(String tokenValue) {
        stpLogic.logoutByTokenValue(tokenValue);
    }
}
