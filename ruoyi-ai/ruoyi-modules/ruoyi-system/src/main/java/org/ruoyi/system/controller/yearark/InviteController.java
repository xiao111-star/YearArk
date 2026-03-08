package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.dto.YaInviteDto;
import org.ruoyi.system.domain.dto.YaInviteQueryDto;
import org.ruoyi.system.domain.vo.YaInviteVo;
import org.ruoyi.system.service.IYaInviteService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 邀请链接接口
 * <p>
 * 所有接口通过 YaUserSecurityConfig 拦截，使用 StpUserUtil 校验登录状态。
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/invite")
public class InviteController extends BaseController {

    private final IYaInviteService inviteService;

    /**
     * 生成邀请链接
     */
    @PostMapping
    public R<Void> create(@Validated @RequestBody YaInviteDto dto) {
        return toAjax(inviteService.createInvite(dto, StpUserUtil.getLoginIdAsInt()));
    }

    /**
     * 查询某纪念册的邀请链接列表
     */
    @GetMapping("/list")
    public R<List<YaInviteVo>> list(@RequestParam Integer albumId) {
        YaInviteQueryDto query = new YaInviteQueryDto();
        query.setAlbumId(albumId);
        return R.ok(inviteService.queryList(query));
    }

    /**
     * 禁用邀请链接
     */
    @PostMapping("/{id}/disable")
    public R<Void> disable(@PathVariable Integer id) {
        return toAjax(inviteService.disableInvite(id, StpUserUtil.getLoginIdAsInt()));
    }
}
