package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.YaTemplateSchemaDto;
import org.ruoyi.system.domain.dto.YaTemplateSchemaQueryDto;
import org.ruoyi.system.domain.vo.YaTemplateSchemaVo;
import org.ruoyi.system.service.IYaTemplateSchemaService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板JSON Schema管理
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/yearark/template-schema")
public class YaTemplateSchemaController extends BaseController {

    private final IYaTemplateSchemaService schemaService;

    /**
     * 分页查询页面Schema列表
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return 页面模板Schema分页数据
     */
    @GetMapping("/page")
    public TableDataInfo<YaTemplateSchemaVo> page(YaTemplateSchemaQueryDto query, PageQuery pageQuery) {
        return schemaService.queryPage(query, pageQuery);
    }

    /**
     * 查询页面Schema列表
     *
     * @param query 查询条件
     * @return 页面模板Schema列表
     */
    @GetMapping("/list")
    public R<List<YaTemplateSchemaVo>> list(YaTemplateSchemaQueryDto query) {
        return R.ok(schemaService.queryList(query));
    }

    /**
     * 根据ID查询页面Schema详情
     *
     * @param id 页面模板SchemaID
     * @return 页面模板Schema详情
     */
    @GetMapping("/{id}")
    public R<YaTemplateSchemaVo> info(@PathVariable Integer id) {
        return R.ok(schemaService.queryById(id));
    }

    /**
     * 新增页面Schema
     *
     * @param dto 页面模板Schema信息
     * @return 操作结果
     */
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaTemplateSchemaDto dto) {
        return toAjax(schemaService.insertByDto(dto));
    }

    /**
     * 修改页面Schema
     *
     * @param dto 页面模板Schema信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaTemplateSchemaDto dto) {
        return toAjax(schemaService.updateByDto(dto));
    }

    /**
     * 批量删除页面Schema
     *
     * @param ids 页面模板SchemaID列表
     * @return 操作结果
     */
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Integer> ids) {
        return toAjax(schemaService.deleteByIds(ids));
    }
}
