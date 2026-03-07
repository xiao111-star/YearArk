package org.ruoyi.system.service.client.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaInvite;
import org.ruoyi.system.domain.YaInviteToken;
import org.ruoyi.system.domain.model.YaAnonUser;
import org.ruoyi.system.domain.vo.AnonTokenVo;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaInviteService;
import org.ruoyi.system.service.IYaInviteTokenService;
import org.ruoyi.system.service.client.IInviteTokenAuthService;
import org.ruoyi.system.util.StpAnonUtil;
import org.ruoyi.system.util.YaLoginHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 匿名 token 认证服务实现
 *
 * @author YearArk
 */
@Service
@RequiredArgsConstructor
public class InviteTokenAuthServiceImpl implements IInviteTokenAuthService {

    private final IYaInviteService yaInviteService;
    private final IYaInviteTokenService yaInviteTokenService;
    private final IYaAlbumService yaAlbumService;

    @Override
    public R<Void> verifyAccessCode(String inviteCode, String accessCode) {
        // 查询 invite 记录
        YaInvite invite = getValidInvite(inviteCode);
        if (invite == null) {
            return R.fail("邀请链接不存在");
        }
        // 校验 accessCode（访问码必填）
        if (!invite.getAccessCode().equals(accessCode)) {
            return R.fail("访问码错误");
        }
        return R.ok();
    }

    @Override
    public R<AnonTokenVo> generateToken(String inviteCode, String ipAddress) {
        // 1. 验证 invite_code 有效（存在、status=0 可用、未过期）
        YaInvite invite = yaInviteService.getOne(
            new LambdaQueryWrapper<YaInvite>()
                .eq(YaInvite::getInviteCode, inviteCode)
        );
        if (invite == null) {
            return R.fail("邀请链接不存在");
        }
        if (!CommonConstants.IS_AVAILABLE.equals(invite.getStatus())) {
            return R.fail("该邀请链接已禁用");
        }
        if (invite.getExpireAt() != null && invite.getExpireAt().isBefore(LocalDateTime.now())) {
            return R.fail("链接已过期");
        }

        // 2. 查询纪念册名称
        YaAlbum album = yaAlbumService.getById(invite.getAlbumId());
        String albumName = album != null ? album.getName() : "";

        // 3. 创建 ya_invite_token 记录（token 字段先占位，登录后回写）
        YaInviteToken inviteToken = new YaInviteToken();
        inviteToken.setAlbumId(invite.getAlbumId());
        inviteToken.setInviteId(invite.getId());
        inviteToken.setIpAddress(ipAddress);
        inviteToken.setStatus(CommonConstants.IS_AVAILABLE);
        inviteToken.setToken("pending"); // 占位，save 后拿到 ID 才能登录生成真实 token
        inviteToken.setExpiredAt(LocalDateTime.now().plusDays(7)); // 匿名 token 7 天有效
        yaInviteTokenService.save(inviteToken);

        // 4. 构建 YaAnonUser 并匿名登录
        YaAnonUser anonUser = new YaAnonUser();
        anonUser.setTokenId(inviteToken.getId());
        anonUser.setAlbumId(invite.getAlbumId());
        anonUser.setInviteId(invite.getId());
        YaLoginHelper.anonLogin(anonUser);

        // 5. 回写 token 到 invite_token 记录
        String tokenValue = StpAnonUtil.getTokenValue();
        inviteToken.setToken(tokenValue);
        yaInviteTokenService.updateById(inviteToken);

        // 6. 构建返回 TokenVo
        AnonTokenVo tokenVo = new AnonTokenVo();
        tokenVo.setToken(tokenValue);
        tokenVo.setAlbumId(invite.getAlbumId());
        tokenVo.setAlbumName(albumName);

        return R.ok(tokenVo);
    }

    /**
     * 查询有效的 invite 记录（存在即返回，不校验状态和过期）
     */
    private YaInvite getValidInvite(String inviteCode) {
        return yaInviteService.getOne(
            new LambdaQueryWrapper<YaInvite>()
                .eq(YaInvite::getInviteCode, inviteCode)
        );
    }
}
