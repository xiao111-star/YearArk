package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.dto.YaAlbumDto;
import org.ruoyi.system.domain.dto.YaAlbumQueryDto;
import org.ruoyi.system.domain.vo.RenderedPageVo;
import org.ruoyi.system.domain.vo.YaAlbumVo;
import org.ruoyi.system.service.AlbumGenerationService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.TemplateRenderService;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 用户端 - 纪念册接口
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/album")
public class UserAlbumController extends BaseController {

    private final IYaAlbumService albumService;
    private final AlbumGenerationService albumGenerationService;
    private final TemplateRenderService templateRenderService;

    @GetMapping("/list")
    public R<List<YaAlbumVo>> list(YaAlbumQueryDto query) {
        query.setUserId(StpUserUtil.getLoginIdAsInt());
        return R.ok(albumService.queryList(query));
    }

    @GetMapping("/{id}")
    public R<YaAlbumVo> info(@PathVariable Integer id) {
        albumService.checkOwnership(id, StpUserUtil.getLoginIdAsInt());
        return R.ok(albumService.queryById(id));
    }

    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaAlbumDto dto) {
        dto.setUserId(StpUserUtil.getLoginIdAsInt());
        return toAjax(albumService.insertByDto(dto));
    }

    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaAlbumDto dto) {
        int userId = StpUserUtil.getLoginIdAsInt();
        albumService.checkOwnership(dto.getId(), userId);
        dto.setUserId(userId);
        return toAjax(albumService.updateByDto(dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Integer id) {
        albumService.checkOwnership(id, StpUserUtil.getLoginIdAsInt());
        return toAjax(albumService.deleteByIds(Collections.singletonList(id)));
    }

    @PostMapping("/{id}/generate")
    public R<Void> generate(@PathVariable Integer id) {
        albumService.checkOwnership(id, StpUserUtil.getLoginIdAsInt());
        return albumGenerationService.generate(id);
    }

    @GetMapping("/{id}/preview")
    public R<List<RenderedPageVo>> preview(@PathVariable Integer id) {
        albumService.checkOwnership(id, StpUserUtil.getLoginIdAsInt());
        return R.ok(templateRenderService.renderAlbum(id));
    }
}