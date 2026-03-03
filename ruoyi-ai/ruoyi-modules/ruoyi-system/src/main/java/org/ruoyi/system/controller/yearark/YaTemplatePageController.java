package org.ruoyi.system.controller.yearark;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.YaTemplatePageDto;
import org.ruoyi.system.domain.dto.YaTemplatePageQueryDto;
import org.ruoyi.system.domain.vo.YaTemplatePageVo;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板页面管理
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/yearark/template-page")
public class YaTemplatePageController extends BaseController {

    private final IYaTemplatePageService templatePageService;

    /**
     * 分页查询模版页面列表
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return 套件页面模板分页数据
     */
    @GetMapping("/page")
    public TableDataInfo<YaTemplatePageVo> page(YaTemplatePageQueryDto query, PageQuery pageQuery) {
        return templatePageService.queryPage(query, pageQuery);
    }

    /**
     * 查询模板页面列表（不分页）
     *
     * @param query 查询条件
     * @return 套件页面模板列表
     */
    @GetMapping("/list")
    public R<List<YaTemplatePageVo>> list(YaTemplatePageQueryDto query) {
        return R.ok(templatePageService.queryList(query));
    }

    /**
     * 根据ID查询模板页面详情
     *
     * @param id 套件页面模板ID
     * @return 套件页面模板详情
     */
    @GetMapping("/{id}")
    public R<YaTemplatePageVo> info(@PathVariable Integer id) {
        return R.ok(templatePageService.queryById(id));
    }

    /**
     * 新增模板页
     *
     * @param dto 套件页面模板信息
     * @return 操作结果
     */
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaTemplatePageDto dto) {
        if (ObjectUtil.isEmpty(dto.getType())){
            return R.fail("请选择页面类型");
        }
        return toAjax(templatePageService.insertByDto(dto));
    }

    /**
     * 修改模板页
     *
     * @param dto 套件页面模板信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaTemplatePageDto dto) {
        return toAjax(templatePageService.updateByDto(dto));
    }

    /**
     * 批量删除模板页
     *
     * @param ids 套件页面模板ID列表
     * @return 操作结果
     */
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Integer> ids) {
        return toAjax(templatePageService.deleteByIds(ids));
    }
}
