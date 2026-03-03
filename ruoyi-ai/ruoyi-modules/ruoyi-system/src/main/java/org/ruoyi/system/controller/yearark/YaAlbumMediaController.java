package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.YaAlbumMediaDto;
import org.ruoyi.system.domain.dto.YaAlbumMediaQueryDto;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;
import org.ruoyi.system.service.IYaAlbumMediaService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纪念册素材接口
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/yearark/album-media")
public class YaAlbumMediaController extends BaseController {

    private final IYaAlbumMediaService mediaService;

    /**
     * 分页查询纪念册素材列表
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return 素材分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<YaAlbumMediaVo> page(YaAlbumMediaQueryDto query, PageQuery pageQuery) {
        return mediaService.queryPage(query, pageQuery);
    }

    /**
     * 查询纪念册素材列表（不分页）
     *
     * @param query 查询条件
     * @return 素材列表
     */
    @GetMapping("/list")
    public R<List<YaAlbumMediaVo>> list(YaAlbumMediaQueryDto query) {
        return R.ok(mediaService.queryList(query));
    }

    /**
     * 获取素材详情
     *
     * @param id 素材ID
     * @return 素材详情
     */
    @GetMapping("/{id}")
    public R<YaAlbumMediaVo> info(@PathVariable Integer id) {
        return R.ok(mediaService.queryById(id));
    }

    /**
     * 新增纪念册素材
     *
     * @param dto 素材信息
     * @return 操作结果
     */
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaAlbumMediaDto dto) {
        return toAjax(mediaService.insertByDto(dto));
    }

    /**
     * 修改纪念册素材
     *
     * @param dto 素材信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaAlbumMediaDto dto) {
        return toAjax(mediaService.updateByDto(dto));
    }

    /**
     * 删除纪念册素材
     *
     * @param ids 素材ID列表
     * @return 操作结果
     */
    //TODO: 删除时，校验是否被使用，被使用则无法删除
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Integer> ids) {
        return toAjax(mediaService.deleteByIds(ids));
    }
}
