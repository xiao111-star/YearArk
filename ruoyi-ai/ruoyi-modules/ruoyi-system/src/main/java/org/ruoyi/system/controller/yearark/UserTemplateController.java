package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.system.domain.dto.YaTemplatePageQueryDto;
import org.ruoyi.system.domain.dto.YaTemplateQueryDto;
import org.ruoyi.system.domain.vo.YaTemplateDetailVo;
import org.ruoyi.system.domain.vo.YaTemplatePageVo;
import org.ruoyi.system.domain.vo.YaTemplateVo;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.ruoyi.system.service.IYaTemplateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端模板查询接口
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/template")
public class UserTemplateController {

    private final IYaTemplateService templateService;
    private final IYaTemplatePageService templatePageService;

    /**
     * 查询所有可用模板列表（名称、预览图、描述）
     */
    @GetMapping("/list")
    public R<List<YaTemplateVo>> list() {
        YaTemplateQueryDto query = new YaTemplateQueryDto();
        query.setStatus(CommonConstants.IS_AVAILABLE);
        return R.ok(templateService.queryList(query));
    }

    /**
     * 模板详情（含模板页列表）n
     */
    @GetMapping("/{id}")
    public R<YaTemplateDetailVo> detail(@PathVariable Integer id) {
        YaTemplateVo template = templateService.queryById(id);
        if (template == null) {
            return R.fail("模板不存在");
        }

        YaTemplatePageQueryDto pageQuery = new YaTemplatePageQueryDto();
        pageQuery.setTemplateId(id);
        List<YaTemplatePageVo> pages = templatePageService.queryList(pageQuery);

        YaTemplateDetailVo vo = new YaTemplateDetailVo();
        vo.setTemplate(template);
        vo.setPages(pages);
        return R.ok(vo);
    }
}
