package org.ruoyi.system.service.client;

import org.ruoyi.common.core.domain.R;
import org.ruoyi.system.domain.vo.AnonTokenVo;

/**
 * 匿名 token 认证服务
 *
 * @author YearArk
 */
public interface IInviteTokenAuthService {

    /**
     * 验证访问码
     *
     * @param inviteCode 邀请码
     * @param accessCode 访问码
     * @return 验证结果
     */
    R<Void> verifyAccessCode(String inviteCode, String accessCode);

    /**
     * 生成匿名 token
     * <p>
     * 验证 invite_code 有效 → 创建 ya_invite_token 记录 → 匿名登录 → 返回 token
     *
     * @param inviteCode 邀请码
     * @param ipAddress  IP 地址
     * @return TokenVo（含 token、albumId、albumName）
     */
    R<AnonTokenVo> generateToken(String inviteCode, String ipAddress);
}
