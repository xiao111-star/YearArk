package org.ruoyi.system.controller.client;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.system.domain.dto.YaUserDto;
import org.ruoyi.system.domain.vo.client.YaLoginVo;
import org.ruoyi.system.service.IYaUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端认证接口
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/auth")
public class UserAuthController {

    private final IYaUserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<Void> register(@Validated @RequestBody YaUserDto dto) {
        return userService.register(dto.getUsername(), dto.getPasswordHash(), dto.getEmail());
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<YaLoginVo> login(@Validated @RequestBody YaUserDto dto) {
        return userService.login(dto.getUsername(), dto.getPasswordHash());
    }

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        return userService.logout();
    }
}
