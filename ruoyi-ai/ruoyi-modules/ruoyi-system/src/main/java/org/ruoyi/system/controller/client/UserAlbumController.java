package org.ruoyi.system.controller.client;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.dto.YaAlbumDto;
import org.ruoyi.system.domain.dto.YaAlbumQueryDto;
import org.ruoyi.system.domain.vo.RenderedPageVo;
import org.ruoyi.system.domain.vo.YaAlbumVo;
import org.ruoyi.system.service.AlbumGenerationService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.TemplateRenderService;
import org.ruoyi.system.util.YaLoginHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 用户端 - 纪念册接口
 * <p>
 * 所有接口通过 YaUserSecurityConfig 拦截，使用 StpUserUtil 校验登录状态。
 * 通过 YaLoginHelper.getUserId() 获取当前用户 ID，实现数据隔离和归属校验。
 *
 * @author ydp
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/album")
public class UserAlbumController extends BaseController {

    private final IYaAlbumService albumService;
    private final AlbumGenerationService albumGenerationService;
    private final TemplateRenderService templateRenderService;

    /**
     * 查询当前用户的纪念册列表
     */
    @GetMapping("/list")
    public R<List<YaAlbumVo>> list(YaAlbumQueryDto query) {
        query.setUserId(YaLoginHelper.getUserId());
        return R.ok(albumService.queryList(query));
    }

    /**
     * 纪念册详情（校验归属）
     */
    @GetMapping("/{id}")
    public R<YaAlbumVo> info(@PathVariable Integer id) {
        checkOwnership(id);
        return R.ok(albumService.queryById(id));
    }

    /**
     * 创建纪念册（自动填充 userId）
     */
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaAlbumDto dto) {
        dto.setUserId(YaLoginHelper.getUserId());
        return toAjax(albumService.insertByDto(dto));
    }

    /**
     * 更新纪念册（校验归属）
     */
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaAlbumDto dto) {
        checkOwnership(dto.getId());
        dto.setUserId(YaLoginHelper.getUserId());
        return toAjax(albumService.updateByDto(dto));
    }

    /**
     * 删除纪念册（单个删除，校验归属）
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Integer id) {
        checkOwnership(id);
        return toAjax(albumService.deleteByIds(Collections.singletonList(id)));
    }

    /**
     * 生成纪念册（校验归属 → 调用 AlbumGenerationService）
     */
    @PostMapping("/{id}/generate")
    public R<Void> generate(@PathVariable Integer id) {
        checkOwnership(id);
        return albumGenerationService.generate(id);
    }

    /**
     * 预览纪念册（校验归属 → 调用 TemplateRenderService）
     */
    @GetMapping("/{id}/preview")
    public R<List<RenderedPageVo>> preview(@PathVariable Integer id) {
        checkOwnership(id);
        return R.ok(templateRenderService.renderAlbum(id));
    }

    /**
     * 校验纪念册归属（album.userId == currentUserId）
     */
    private void checkOwnership(Integer albumId) {
        YaAlbumVo album = albumService.queryById(albumId);
        if (album == null || !album.getUserId().equals(YaLoginHelper.getUserId())) {
            throw new ServiceException("无权操作该纪念册");
        }
    }
}
