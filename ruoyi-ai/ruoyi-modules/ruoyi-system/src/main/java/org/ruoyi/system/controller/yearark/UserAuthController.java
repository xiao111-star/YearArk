package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.dto.ChangePasswordDto;
import org.ruoyi.system.domain.dto.UserProfileDto;
import org.ruoyi.system.domain.dto.YaUserLoginDto;
import org.ruoyi.system.domain.dto.YaUserRegisterDto;
import org.ruoyi.system.domain.vo.YaUserVo;
import org.ruoyi.system.domain.vo.client.YaLoginVo;
import org.ruoyi.system.service.ISysOssService;
import org.ruoyi.system.service.IYaUserService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户认证接口
 * <p>
 * 路径：/api/user/auth
 * <p>
 * 包含注册、登录、登出、获取用户信息四个接口
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/auth")
public class UserAuthController extends BaseController {

    private final IYaUserService yaUserService;
    private final ISysOssService sysOssService;

    /**
     * 用户注册
     *
     * @param dto 注册信息（passwordHash 为前端 SHA-256 后的哈希值）
     * @return 注册结果
     */
    @PostMapping("/register")
    public R<Void> register(@Validated @RequestBody YaUserRegisterDto dto) {
        return yaUserService.register(dto);
    }

    /**
     * 用户登录
     *
     * @param dto 登录信息（passwordHash 为前端 SHA-256 后的哈希值）
     * @return 登录结果（含 token 和用户信息）
     */
    @PostMapping("/login")
    public R<YaLoginVo> login(@Validated @RequestBody YaUserLoginDto dto) {
        return yaUserService.login(dto);
    }

    /**
     * 用户登出
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        return yaUserService.logout();
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 需要 Ya-Auth token。从 token 中解析 userId，查询用户信息。
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public R<YaUserVo> getUserInfo() {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        YaUserVo userVo = yaUserService.queryById(userId);
        if (userVo == null) {
            return R.fail("用户不存在");
        }
        return R.ok(userVo);
    }

    /**
     * 更新个人资料（用户名、邮箱）
     */
    @PostMapping("/profile")
    public R<Void> updateProfile(@Validated @RequestBody UserProfileDto dto) {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        return yaUserService.updateProfile(userId, dto);
    }

    /**
     * 上传头像
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        Integer userId = StpUserUtil.getLoginIdAsInt();
        String url = sysOssService.upload(file).getUrl();
        yaUserService.updateAvatar(userId, url);
        return R.ok("上传成功", url);
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public R<Void> changePassword(@Validated @RequestBody ChangePasswordDto dto) {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        return yaUserService.changePassword(userId, dto);
    }
}
