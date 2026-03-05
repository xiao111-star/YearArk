package org.ruoyi.system.controller.yearark;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.YaTemplateDto;
import org.ruoyi.system.domain.dto.YaTemplateQueryDto;
import org.ruoyi.system.domain.vo.YaTemplateVo;
import org.ruoyi.system.service.IYaTemplateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板管理
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/yearark/template")
public class YaTemplateController extends BaseController {

    private final IYaTemplateService templateService;

    /**
     * 分页查询模板列表
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return 模板套件分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<YaTemplateVo> page(YaTemplateQueryDto query, PageQuery pageQuery) {
        return templateService.queryPage(query, pageQuery);
    }

    /**
     * 查询模板列表（不分页）
     *
     * @param query 查询条件
     * @return 模板套件列表
     */
    @GetMapping("/list")
    public R<List<YaTemplateVo>> list(YaTemplateQueryDto query) {
        return R.ok(templateService.queryList(query));
    }

    /**
     * 获取模板详情
     *
     * @param id 模板套件ID
     * @return 模板套件详情
     */
    @GetMapping("/{id}")
    public R<YaTemplateVo> info(@PathVariable Integer id) {
        return R.ok(templateService.queryById(id));
    }

    /**
     * 新增模板
     *
     * @param dto 模板套件信息
     * @return 操作结果
     */
    @SaCheckPermission("yearark:template:add")
    @Log(title = "模板套件管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaTemplateDto dto) {
        return toAjax(templateService.insertByDto(dto));
    }

    /**
     * 修改模板
     *
     * @param dto 模板套件信息
     * @return 操作结果
     */
    @SaCheckPermission("yearark:template:edit")
    @Log(title = "模板套件管理", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaTemplateDto dto) {
        return toAjax(templateService.updateByDto(dto));
    }

    /**
     * 删除模板
     *
     * @param ids 模板套件ID列表
     * @return 操作结果
     */
    @SaCheckPermission("yearark:template:remove")
    @Log(title = "模板套件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Integer> ids) {
        return toAjax(templateService.deleteByIds(ids));
    }
}
