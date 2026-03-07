package org.ruoyi.system.util;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.session.SaSession;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.ruoyi.system.domain.model.YaAnonUser;
import org.ruoyi.system.domain.model.YaLoginUser;

/**
 * 用户端登录鉴权助手
 * <p>
 * 参照管理端 LoginHelper 模式，封装用户端的登录/获取用户信息逻辑。
 * 支持两种身份体系：
 * <ul>
 *   <li>已登录用户（ya_user）：通过 StpUserUtil 管理</li>
 *   <li>匿名上传者（ya_invite_token）：通过 StpAnonUtil 管理</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class YaLoginHelper {

    public static final String LOGIN_USER_KEY = "yaLoginUser";
    public static final String USER_KEY = "yaUserId";
    public static final String ANON_USER_KEY = "anonUser";

    /**
     * 用户端登录（ya_user）
     */
    public static void login(YaLoginUser loginUser) {
        SaStorage storage = SaHolder.getStorage();
        storage.set(LOGIN_USER_KEY, loginUser);
        storage.set(USER_KEY, loginUser.getUserId());
        // 直接使用 userId 作为 loginId，StpUserUtil 已是独立 StpLogic，无需前缀
        StpUserUtil.login(loginUser.getUserId());
        StpUserUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * 获取当前登录的 ya_user 信息（多级缓存）
     */
    public static YaLoginUser getLoginUser() {
        YaLoginUser loginUser = (YaLoginUser) SaHolder.getStorage().get(LOGIN_USER_KEY);
        if (loginUser != null) {
            return loginUser;
        }
        SaSession tokenSession = StpUserUtil.getTokenSession();
        if (tokenSession != null) {
            loginUser = (YaLoginUser) tokenSession.get(LOGIN_USER_KEY);
            SaHolder.getStorage().set(LOGIN_USER_KEY, loginUser);
        }
        return loginUser;
    }

    /**
     * 获取当前 ya_user 的 userId
     */
    public static Integer getUserId() {
        Integer userId;
        try {
            userId = Convert.toInt(SaHolder.getStorage().get(USER_KEY));
            if (ObjectUtil.isNull(userId)) {
                userId = Convert.toInt(StpUserUtil.getLoginId());
                SaHolder.getStorage().set(USER_KEY, userId);
            }
        } catch (Exception e) {
            return null;
        }
        return userId;
    }

    /**
     * 匿名用户登录（ya_invite_token）
     */
    public static void anonLogin(YaAnonUser anonUser) {
        StpAnonUtil.login(anonUser.getTokenId());
        StpAnonUtil.getTokenSession().set(ANON_USER_KEY, anonUser);
    }

    /**
     * 获取当前匿名用户信息
     */
    public static YaAnonUser getAnonUser() {
        SaSession tokenSession = StpAnonUtil.getTokenSession();
        if (tokenSession != null) {
            return (YaAnonUser) tokenSession.get(ANON_USER_KEY);
        }
        return null;
    }
}
