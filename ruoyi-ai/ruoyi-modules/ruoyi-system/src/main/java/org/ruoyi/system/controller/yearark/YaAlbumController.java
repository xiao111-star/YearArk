package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.YaAlbumDto;
import org.ruoyi.system.domain.dto.YaAlbumQueryDto;
import org.ruoyi.system.domain.vo.YaAlbumVo;
import org.ruoyi.system.service.IYaAlbumService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纪念册接口
 *
 * @author ydp
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/yearark/album")
public class YaAlbumController extends BaseController {

    private final IYaAlbumService albumService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public TableDataInfo<YaAlbumVo> page(YaAlbumQueryDto query, PageQuery pageQuery) {
        return albumService.queryPage(query, pageQuery);
    }

    /**
     * 列表查询（不分页）
     */
    @GetMapping("/list")
    public R<List<YaAlbumVo>> list(YaAlbumQueryDto query) {
        return R.ok(albumService.queryList(query));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<YaAlbumVo> info(@PathVariable Integer id) {
        return R.ok(albumService.queryById(id));
    }

    /**
     * 新增纪念册
     *
     * @param dto 纪念册信息
     * @return 操作结果
     */
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaAlbumDto dto) {
        return toAjax(albumService.insertByDto(dto));
    }

    /**
     * 修改纪念册
     *
     * @param dto 纪念册信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaAlbumDto dto) {
        return toAjax(albumService.updateByDto(dto));
    }

    /**
     * 删除纪念册
     *
     * @param ids 纪念册ID列表
     * @return 操作结果
     */
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Integer> ids) {
        return toAjax(albumService.deleteByIds(ids));
    }
}
