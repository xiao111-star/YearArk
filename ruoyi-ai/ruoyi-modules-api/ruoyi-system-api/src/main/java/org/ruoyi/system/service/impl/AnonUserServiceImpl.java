package org.ruoyi.system.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.satoken.utils.StpAnonUtil;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaAlbumMedia;
import org.ruoyi.system.domain.YaInvite;
import org.ruoyi.system.domain.YaInviteToken;
import org.ruoyi.system.domain.YaAnonUser;
import org.ruoyi.system.domain.vo.AnonAlbumInfoVo;
import org.ruoyi.system.domain.vo.AnonTokenVo;
import org.ruoyi.system.domain.vo.SysOssVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;
import org.ruoyi.system.service.IAnonUserService;
import org.ruoyi.system.service.ISysOssService;
import org.ruoyi.system.service.IYaAlbumMediaService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaInviteService;
import org.ruoyi.system.service.IYaInviteTokenService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 匿名用户服务实现
 *
 * @author YearArk
 */
@Service
@RequiredArgsConstructor
public class AnonUserServiceImpl implements IAnonUserService {

    private final IYaInviteService inviteService;
    private final IYaInviteTokenService inviteTokenService;
    private final IYaAlbumService albumService;
    private final IYaAlbumMediaService mediaService;
    private final ISysOssService ossService;

    @Override
    public R<AnonAlbumInfoVo> getShareInfo(String inviteCode) {
        YaInvite invite = inviteService.getOne(
            new LambdaQueryWrapper<YaInvite>().eq(YaInvite::getInviteCode, inviteCode)
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

        YaAlbum album = albumService.getById(invite.getAlbumId());
        if (album == null) {
            return R.fail("纪念册不存在");
        }

        AnonAlbumInfoVo vo = new AnonAlbumInfoVo();
        vo.setAlbumName(album.getName());
        vo.setAlbumDes(album.getDes());
        return R.ok(vo);
    }

    @Override
    public R<AnonTokenVo> verifyAndGenerateToken(String inviteCode, String accessCode, String ipAddress) {
        if (StrUtil.isBlank(accessCode)) {
            return R.fail("请输入访问码");
        }

        // 1. 查询并校验 invite
        YaInvite invite = inviteService.getOne(
            new LambdaQueryWrapper<YaInvite>().eq(YaInvite::getInviteCode, inviteCode)
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

        // 2. 校验访问码
        if (!invite.getAccessCode().equals(accessCode)) {
            return R.fail("访问码错误");
        }

        // 3. 查询纪念册名称
        YaAlbum album = albumService.getById(invite.getAlbumId());
        String albumName = album != null ? album.getName() : "";

        // 4. 创建 invite_token 记录（token 字段先占位，登录后回写）
        YaInviteToken inviteToken = new YaInviteToken();
        inviteToken.setAlbumId(invite.getAlbumId());
        inviteToken.setInviteId(invite.getId());
        inviteToken.setIpAddress(ipAddress);
        inviteToken.setStatus(CommonConstants.IS_AVAILABLE);
        inviteToken.setToken("pending");
        inviteToken.setExpiredAt(LocalDateTime.now().plusDays(7));
        inviteTokenService.save(inviteToken);

        // 5. 匿名登录，将 anonUser 存入 token session
        YaAnonUser anonUser = new YaAnonUser();
        anonUser.setTokenId(inviteToken.getId());
        anonUser.setAlbumId(invite.getAlbumId());
        anonUser.setInviteId(invite.getId());
        StpAnonUtil.login(inviteToken.getId());
        StpAnonUtil.getTokenSession().set("anonUser", anonUser);

        // 6. 回写真实 token 值
        String tokenValue = StpAnonUtil.getTokenValue();
        inviteToken.setToken(tokenValue);
        inviteTokenService.updateById(inviteToken);

        AnonTokenVo tokenVo = new AnonTokenVo();
        tokenVo.setToken(tokenValue);
        tokenVo.setAlbumId(invite.getAlbumId());
        tokenVo.setAlbumName(albumName);
        return R.ok(tokenVo);
    }

    @Override
    public R<YaAlbumMediaVo> uploadImage(MultipartFile file) {
        if (ObjectUtil.isNull(file) || file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }

        // 检查 token 是否在黑名单中
        R<Void> blacklistCheck = checkTokenBlacklist();
        if (R.isError(blacklistCheck)) {
            return R.fail(blacklistCheck.getMsg());
        }

        YaAnonUser anonUser = getCurrentAnonUser();

        SysOssVo ossVo = ossService.upload(file);

        YaAlbumMedia media = new YaAlbumMedia();
        media.setAlbumId(anonUser.getAlbumId());
        media.setTokenId(anonUser.getTokenId());
        media.setType(2);
        media.setContent(ossVo.getUrl());
        media.setStatus(2);
        media.setSize((double) file.getSize() / (1024 * 1024));
        media.setCreateAt(LocalDateTime.now());
        media.setUpdateAt(LocalDateTime.now());
        media.setIsDelete(CommonConstants.NOT_DELETE);
        mediaService.save(media);

        return R.ok(BeanUtil.copyProperties(media, YaAlbumMediaVo.class));
    }

    @Override
    public R<YaAlbumMediaVo> uploadText(String content) {
        // 检查 token 是否在黑名单中
        R<Void> blacklistCheck = checkTokenBlacklist();
        if (R.isError(blacklistCheck)) {
            return R.fail(blacklistCheck.getMsg());
        }

        YaAnonUser anonUser = getCurrentAnonUser();

        YaAlbumMedia media = new YaAlbumMedia();
        media.setAlbumId(anonUser.getAlbumId());
        media.setTokenId(anonUser.getTokenId());
        media.setType(1);
        media.setContent(content);
        media.setStatus(2);
        media.setCreateAt(LocalDateTime.now());
        media.setUpdateAt(LocalDateTime.now());
        media.setIsDelete(CommonConstants.NOT_DELETE);
        mediaService.save(media);

        return R.ok(BeanUtil.copyProperties(media, YaAlbumMediaVo.class));
    }

    @Override
    public R<List<YaAlbumMediaVo>> getMyUploads() {
        // 检查 token 是否在黑名单中
        R<Void> blacklistCheck = checkTokenBlacklist();
        if (R.isError(blacklistCheck)) {
            return R.fail(blacklistCheck.getMsg());
        }

        YaAnonUser anonUser = getCurrentAnonUser();

        List<YaAlbumMedia> list = mediaService.list(
            new LambdaQueryWrapper<YaAlbumMedia>()
                .eq(YaAlbumMedia::getAlbumId, anonUser.getAlbumId())
                .eq(YaAlbumMedia::getTokenId, anonUser.getTokenId())
                .orderByDesc(YaAlbumMedia::getCreateAt)
        );
        return R.ok(BeanUtil.copyToList(list, YaAlbumMediaVo.class));
    }

    @Override
    public R<Void> deleteMyUpload(Long mediaId) {
        // 检查 token 是否在黑名单中
        R<Void> blacklistCheck = checkTokenBlacklist();
        if (R.isError(blacklistCheck)) {
            return blacklistCheck;
        }

        YaAnonUser anonUser = getCurrentAnonUser();

        YaAlbumMedia media = mediaService.getById(mediaId);
        if (media == null) {
            return R.fail("素材不存在");
        }
        // 只能删除自己上传的素材
        if (!media.getTokenId().equals(anonUser.getTokenId())
            || !media.getAlbumId().equals(anonUser.getAlbumId())) {
            return R.fail("无权删除该素材");
        }

        mediaService.removeById(mediaId);
        return R.ok();
    }

    /**
     * 检查当前 token 是否在黑名单中
     */
    private R<Void> checkTokenBlacklist() {
        try {
            String tokenValue = StpAnonUtil.getTokenValue();
            if (StrUtil.isBlank(tokenValue)) {
                return R.fail("未登录");
            }
            
            String blacklistKey = "invite:token:blacklist:" + tokenValue;
            Boolean isBlacklisted = org.ruoyi.common.redis.utils.RedisUtils.getCacheObject(blacklistKey);
            
            if (Boolean.TRUE.equals(isBlacklisted)) {
                // Token 在黑名单中，强制登出
                StpAnonUtil.logout();
                return R.fail("该邀请链接已被禁用，请联系管理员");
            }
            
            return R.ok();
        } catch (Exception e) {
            return R.fail("身份验证失败");
        }
    }

    /**
     * 从当前 token session 中获取匿名用户信息
     */
    private YaAnonUser getCurrentAnonUser() {
        SaSession session = StpAnonUtil.getTokenSession();
        if (session == null) {
            throw new ServiceException("匿名身份信息获取失败");
        }
        YaAnonUser anonUser = (YaAnonUser) session.get("anonUser");
        if (anonUser == null) {
            throw new ServiceException("匿名身份信息获取失败");
        }
        return anonUser;
    }
}
