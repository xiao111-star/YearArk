package org.ruoyi.system.controller.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.system.domain.dto.UploadTextDto;
import org.ruoyi.system.domain.vo.AnonAlbumInfoVo;
import org.ruoyi.system.domain.vo.AnonTokenVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;
import org.ruoyi.system.service.client.IAnonUserService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 匿名用户接口（公开 + 匿名认证）
 * <p>
 * 路径：/api/user/share/**
 * <p>
 * 公开接口（无需认证）：
 * - GET /{inviteCode} - 验证邀请码，获取纪念册信息
 * - POST /{inviteCode}/verify - 验证访问码，生成匿名 token
 * <p>
 * 匿名认证接口（需要 Ya-Anon-Auth token，通过 YaUserSecurityConfig 拦截）：
 * - POST /upload/image - 匿名上传图片
 * - POST /upload/text - 匿名上传文字
 * - GET /my-uploads - 查询当前匿名用户的上传列表
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/share")
public class AnonUserController {

    private final IAnonUserService anonUserService;

    /**
     * 验证邀请码，返回纪念册基本信息
     *
     * @param inviteCode 邀请码
     * @return ShareInfoVo（纪念册名称、描述）
     */
    @GetMapping("/{inviteCode}")
    public R<AnonAlbumInfoVo> getShareInfo(@PathVariable String inviteCode) {
        return anonUserService.getShareInfo(inviteCode);
    }

    /**
     * 验证访问码 → 生成匿名 token
     *
     * @param inviteCode 邀请码
     * @param body       请求体，包含 accessCode（可选）
     * @param request    HttpServletRequest（获取 IP）
     * @return TokenVo（含 StpAnonUtil token）
     */
    @PostMapping("/{inviteCode}/verify")
    public R<AnonTokenVo> verify(@PathVariable String inviteCode,
                                 @RequestBody(required = false) Map<String, String> body,
                                 HttpServletRequest request) {
        String accessCode = (body != null) ? body.get("accessCode") : null;
        String ipAddress = request.getRemoteAddr();
        return anonUserService.verifyAndGenerateToken(inviteCode, accessCode, ipAddress);
    }

    /**
     * 匿名上传图片
     * <p>
     * 需要 Ya-Anon-Auth token。上传图片到 OSS，创建 media 记录（type=2, status=2）。
     *
     * @param file 图片文件
     * @return 创建的素材信息
     */
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<YaAlbumMediaVo> uploadImage(@RequestPart("file") MultipartFile file) {
        return anonUserService.uploadImage(file);
    }

    /**
     * 匿名上传文字
     * <p>
     * 需要 Ya-Anon-Auth token。创建 media 记录（type=1, status=2）。
     *
     * @param dto 文字内容
     * @return 创建的素材信息
     */
    @PostMapping("/upload/text")
    public R<YaAlbumMediaVo> uploadText(@Validated @RequestBody UploadTextDto dto) {
        return anonUserService.uploadText(dto.getContent());
    }

    /**
     * 查询当前匿名用户已上传的素材列表
     * <p>
     * 需要 Ya-Anon-Auth token。根据 tokenId 查询该 token 上传的所有素材。
     *
     * @return 素材列表
     */
    @GetMapping("/my-uploads")
    public R<List<YaAlbumMediaVo>> myUploads() {
        return anonUserService.getMyUploads();
    }
}
