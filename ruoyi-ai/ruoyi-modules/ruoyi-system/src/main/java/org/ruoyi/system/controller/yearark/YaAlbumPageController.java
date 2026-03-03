package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.YaAlbumPageDto;
import org.ruoyi.system.domain.dto.YaAlbumPageQueryDto;
import org.ruoyi.system.domain.vo.YaAlbumPageVo;
import org.ruoyi.system.service.IYaAlbumPageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纪念册页面管理
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/yearark/album-page")
public class YaAlbumPageController extends BaseController {

    private final IYaAlbumPageService pageService;

    /**
     * 分页查询纪念册页面列表
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return 页面分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<YaAlbumPageVo> page(YaAlbumPageQueryDto query, PageQuery pageQuery) {
        return pageService.queryPage(query, pageQuery);
    }

    /**
     * 查询纪念册页面列表（不分页）
     *
     * @param query 查询条件
     * @return 页面列表
     */
    @GetMapping("/list")
    public R<List<YaAlbumPageVo>> list(YaAlbumPageQueryDto query) {
        return R.ok(pageService.queryList(query));
    }

    /**
     * 获取纪念册页面详情
     *
     * @param id 页面ID
     * @return 页面详情
     */
    @GetMapping("/{id}")
    public R<YaAlbumPageVo> info(@PathVariable Integer id) {
        return R.ok(pageService.queryById(id));
    }

    /**
     * 新增纪念册页面
     *
     * @param dto 页面信息
     * @return 操作结果
     */
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaAlbumPageDto dto) {
        return toAjax(pageService.insertByDto(dto));
    }

    /**
     * 修改纪念册页面
     *
     * @param dto 页面信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaAlbumPageDto dto) {
        return toAjax(pageService.updateByDto(dto));
    }

    /**
     * 删除纪念册页面
     *
     * @param ids 页面ID列表
     * @return 操作结果
     */
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Integer> ids) {
        return toAjax(pageService.deleteByIds(ids));
    }
}
