package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.dto.PageUpdateDto;
import org.ruoyi.system.domain.dto.YaAlbumDto;
import org.ruoyi.system.domain.dto.YaAlbumQueryDto;
import org.ruoyi.system.domain.vo.EditablePageVo;
import org.ruoyi.system.domain.vo.RenderedPageVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;
import org.ruoyi.system.domain.vo.YaAlbumVo;
import org.ruoyi.system.service.AlbumGenerationService;
import org.ruoyi.system.service.AlbumPageEditService;
import org.ruoyi.system.service.IYaAlbumMediaService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.TemplateRenderService;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用户端 - 纪念册接口
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/album")
public class UserAlbumController extends BaseController {

    private final IYaAlbumService albumService;
    private final AlbumGenerationService albumGenerationService;
    private final TemplateRenderService templateRenderService;
    private final AlbumPageEditService albumPageEditService;
    private final IYaAlbumMediaService albumMediaService;

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
    public R<Integer> insert(@Validated @RequestBody YaAlbumDto dto) {
        dto.setUserId(StpUserUtil.getLoginIdAsInt());
        return R.ok(albumService.insertByDto(dto));
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

    @GetMapping("/{id}/status")
    public R<YaAlbumVo> status(@PathVariable Integer id) {
        albumService.checkOwnership(id, StpUserUtil.getLoginIdAsInt());
        return R.ok(albumService.getGenerationStatus(id));
    }

    @GetMapping("/{id}/edit-data")
    public R<List<EditablePageVo>> editData(@PathVariable Integer id) {
        albumService.checkOwnership(id, StpUserUtil.getLoginIdAsInt());
        return R.ok(albumPageEditService.getEditData(id));
    }

    @PutMapping("/page/{pageId}")
    public R<RenderedPageVo> updatePage(@PathVariable Integer pageId,
                                        @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) body.get("data");
        if (dataMap == null) return R.fail("data 不能为空");
        return R.ok(albumPageEditService.updatePageData(pageId, StpUserUtil.getLoginIdAsInt(), dataMap));
    }

    @PutMapping("/{id}/pages")
    public R<List<RenderedPageVo>> batchUpdatePages(@PathVariable Integer id,
                                                    @RequestBody List<PageUpdateDto> updates) {
        albumService.checkOwnership(id, StpUserUtil.getLoginIdAsInt());
        return R.ok(albumPageEditService.batchUpdatePageData(updates));
    }

    @GetMapping("/{id}/unused-media")
    public R<List<YaAlbumMediaVo>> unusedMedia(@PathVariable Integer id) {
        albumService.checkOwnership(id, StpUserUtil.getLoginIdAsInt());
        return R.ok(albumMediaService.listUnusedImages(id));
    }

    @PostMapping(value = "/{id}/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<YaAlbumMediaVo> uploadMedia(@PathVariable Integer id,
                                         @RequestPart("file") MultipartFile file) {
        return R.ok(albumMediaService.uploadImage(id, StpUserUtil.getLoginIdAsInt(), file));
    }

    @DeleteMapping("/media/{mediaId}")
    public R<Void> deleteMedia(@PathVariable Integer mediaId) {
        albumMediaService.deleteMediaByUser(mediaId, StpUserUtil.getLoginIdAsInt());
        return R.ok();
    }
}
