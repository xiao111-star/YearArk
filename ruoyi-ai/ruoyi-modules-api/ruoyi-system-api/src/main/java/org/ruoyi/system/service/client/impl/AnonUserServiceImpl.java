package org.ruoyi.system.service.client.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaAlbumMedia;
import org.ruoyi.system.domain.YaInvite;
import org.ruoyi.system.domain.model.YaAnonUser;
import org.ruoyi.system.domain.vo.AnonAlbumInfoVo;
import org.ruoyi.system.domain.vo.SysOssVo;
import org.ruoyi.system.domain.vo.AnonTokenVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;
import org.ruoyi.system.service.IYaAlbumMediaService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaInviteService;
import org.ruoyi.system.service.ISysOssService;
import org.ruoyi.system.service.client.IAnonUserService;
import org.ruoyi.system.service.client.IInviteTokenAuthService;
import org.ruoyi.system.util.YaLoginHelper;
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
    private final IYaAlbumService albumService;
    private final IYaAlbumMediaService mediaService;
    private final ISysOssService ossService;
    private final IInviteTokenAuthService inviteTokenAuthService;

    @Override
    public R<AnonAlbumInfoVo> getShareInfo(String inviteCode) {
        // 查询 invite 记录
        YaInvite invite = inviteService.getOne(
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

        // 查询纪念册信息
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
        // 访问码必填，验证访问码
        if (StrUtil.isBlank(accessCode)) {
            return R.fail("请输入访问码");
        }
        
        R<Void> verifyResult = inviteTokenAuthService.verifyAccessCode(inviteCode, accessCode);
        if (!R.isSuccess(verifyResult)) {
            return R.fail(verifyResult.getMsg());
        }

        // 生成匿名 token
        return inviteTokenAuthService.generateToken(inviteCode, ipAddress);
    }

    @Override
    public R<YaAlbumMediaVo> uploadImage(MultipartFile file) {
        // 参数校验
        if (ObjectUtil.isNull(file) || file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }

        // 获取匿名用户信息
        YaAnonUser anonUser = YaLoginHelper.getAnonUser();
        if (anonUser == null) {
            throw new ServiceException("匿名身份信息获取失败");
        }

        // 上传到 OSS
        SysOssVo ossVo = ossService.upload(file);

        // 创建 media 记录
        YaAlbumMedia media = new YaAlbumMedia();
        media.setAlbumId(anonUser.getAlbumId());
        media.setTokenId(anonUser.getTokenId());
        media.setType(2); // 图片
        media.setContent(ossVo.getUrl());
        media.setStatus(2); // 直接通过
        media.setSize((double) file.getSize() / (1024 * 1024)); // 转 MB
        media.setCreateAt(LocalDateTime.now());
        media.setUpdateAt(LocalDateTime.now());
        media.setIsDelete(CommonConstants.NOT_DELETE);
        mediaService.save(media);

        // 构建返回 VO
        YaAlbumMediaVo vo = BeanUtil.copyProperties(media, YaAlbumMediaVo.class);
        return R.ok(vo);
    }

    @Override
    public R<YaAlbumMediaVo> uploadText(String content) {
        // 参数校验在 DTO 中完成，这里直接使用
        YaAnonUser anonUser = YaLoginHelper.getAnonUser();
        if (anonUser == null) {
            throw new ServiceException("匿名身份信息获取失败");
        }

        // 创建 media 记录
        YaAlbumMedia media = new YaAlbumMedia();
        media.setAlbumId(anonUser.getAlbumId());
        media.setTokenId(anonUser.getTokenId());
        media.setType(1); // 文本
        media.setContent(content);
        media.setStatus(2); // 直接通过
        media.setCreateAt(LocalDateTime.now());
        media.setUpdateAt(LocalDateTime.now());
        media.setIsDelete(CommonConstants.NOT_DELETE);
        mediaService.save(media);

        // 构建返回 VO
        YaAlbumMediaVo vo = BeanUtil.copyProperties(media, YaAlbumMediaVo.class);
        return R.ok(vo);
    }

    @Override
    public R<List<YaAlbumMediaVo>> getMyUploads() {
        YaAnonUser anonUser = YaLoginHelper.getAnonUser();
        if (anonUser == null) {
            throw new ServiceException("匿名身份信息获取失败");
        }

        // 根据 tokenId 查询该匿名用户上传的所有素材
        List<YaAlbumMedia> list = mediaService.list(
            new LambdaQueryWrapper<YaAlbumMedia>()
                .eq(YaAlbumMedia::getAlbumId, anonUser.getAlbumId())
                .eq(YaAlbumMedia::getTokenId, anonUser.getTokenId())
                .orderByDesc(YaAlbumMedia::getCreateAt)
        );
        List<YaAlbumMediaVo> voList = BeanUtil.copyToList(list, YaAlbumMediaVo.class);
        return R.ok(voList);
    }
}
