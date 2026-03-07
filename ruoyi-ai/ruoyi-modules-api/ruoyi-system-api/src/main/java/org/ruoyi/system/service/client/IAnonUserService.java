package org.ruoyi.system.service.client;

import org.ruoyi.common.core.domain.R;
import org.ruoyi.system.domain.vo.AnonAlbumInfoVo;
import org.ruoyi.system.domain.vo.AnonTokenVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 匿名用户服务接口
 * <p>
 * 处理匿名用户相关的业务逻辑：
 * - 邀请码验证
 * - 匿名 token 生成
 * - 匿名上传（图片/文字）
 * - 查询匿名用户上传列表
 *
 * @author YearArk
 */
public interface IAnonUserService {

    /**
     * 获取分享信息（验证邀请码）
     *
     * @param inviteCode 邀请码
     * @return 纪念册基本信息
     */
    R<AnonAlbumInfoVo> getShareInfo(String inviteCode);

    /**
     * 验证访问码并生成匿名 token
     *
     * @param inviteCode 邀请码
     * @param accessCode 访问码（可选）
     * @param ipAddress  IP 地址
     * @return 匿名 token 信息
     */
    R<AnonTokenVo> verifyAndGenerateToken(String inviteCode, String accessCode, String ipAddress);

    /**
     * 匿名上传图片
     *
     * @param file 图片文件
     * @return 创建的素材信息
     */
    R<YaAlbumMediaVo> uploadImage(MultipartFile file);

    /**
     * 匿名上传文字
     *
     * @param content 文字内容
     * @return 创建的素材信息
     */
    R<YaAlbumMediaVo> uploadText(String content);

    /**
     * 查询当前匿名用户已上传的素材列表
     *
     * @return 素材列表
     */
    R<List<YaAlbumMediaVo>> getMyUploads();
}
