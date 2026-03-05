package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaTemplatePage;
import org.ruoyi.system.domain.YaTemplateSchema;
import org.ruoyi.system.domain.dto.YaTemplateSchemaDto;
import org.ruoyi.system.domain.dto.YaTemplateSchemaQueryDto;
import org.ruoyi.system.domain.vo.YaTemplateSchemaVo;
import org.ruoyi.system.mapper.YaTemplateSchemaMapper;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.ruoyi.system.service.IYaTemplateSchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YaTemplateSchemaServiceImpl extends ServiceImpl<YaTemplateSchemaMapper, YaTemplateSchema>
        implements IYaTemplateSchemaService {

    @Autowired
    private IYaTemplatePageService templatePageService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public TableDataInfo<YaTemplateSchemaVo> queryPage(YaTemplateSchemaQueryDto query, PageQuery pageQuery) {
        Page<YaTemplateSchema> page = this.page(pageQuery.build(), buildWrapper(query));
        List<YaTemplateSchemaVo> voList = BeanUtil.copyToList(page.getRecords(), YaTemplateSchemaVo.class);
        fillUsageCount(voList);
        return new TableDataInfo<>(voList, page.getTotal());
    }

    @Override
    public List<YaTemplateSchemaVo> queryList(YaTemplateSchemaQueryDto query) {
        return BeanUtil.copyToList(this.list(buildWrapper(query)), YaTemplateSchemaVo.class);
    }

    @Override
    public YaTemplateSchemaVo queryById(Integer id) {
        YaTemplateSchema entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaTemplateSchemaVo.class);
    }

    @Override
    public boolean insertByDto(YaTemplateSchemaDto dto) {
        if(StringUtils.isBlank(dto.getContent())){
            throw new ServiceException("请输入模板内容");
        }
        if(ObjectUtil.isEmpty(dto.getImageCount())){
            throw new ServiceException("请输入图片数量");
        }
        if(ObjectUtil.isEmpty(dto.getTextCount())){
            throw new ServiceException("请输入文本数量");
        }
        validateJsonContent(dto.getContent());
        YaTemplateSchema yaTemplateSchema = BeanUtil.toBean(dto, YaTemplateSchema.class);
        yaTemplateSchema.setStatus(dto.getStatus()!=null?dto.getStatus(): CommonConstants.IS_AVAILABLE);
        Long userId = LoginHelper.getUserId();
        if (userId != null) {
            yaTemplateSchema.setCreateBy(String.valueOf(userId));
            yaTemplateSchema.setUpdateBy(String.valueOf(userId));
        }
        yaTemplateSchema.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(yaTemplateSchema);
    }

    @Override
    public boolean updateByDto(YaTemplateSchemaDto dto) {
        validateJsonContent(dto.getContent());
        YaTemplateSchema yaTemplateSchema = BeanUtil.toBean(dto, YaTemplateSchema.class);
        Long userId = LoginHelper.getUserId();
        if (userId != null) {
            yaTemplateSchema.setUpdateBy(String.valueOf(userId));
        }
        return this.updateById(yaTemplateSchema);
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        // 检查是否有 TemplatePage 正在使用这些 Schema
        for (Integer schemaId : ids) {
            long count = templatePageService.count(
                new LambdaQueryWrapper<YaTemplatePage>().eq(YaTemplatePage::getTemplateSchemaId, schemaId)
            );
            if (count > 0) {
                throw new ServiceException("该 Schema 已被模板页面使用，无法删除");
            }
        }
        return this.removeByIds(ids);
    }

    private void validateJsonContent(String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        try {
            OBJECT_MAPPER.readTree(content);
        } catch (JsonProcessingException e) {
            long line = e.getLocation() != null ? e.getLocation().getLineNr() : -1;
            long column = e.getLocation() != null ? e.getLocation().getColumnNr() : -1;
            throw new ServiceException("JSON 格式不正确：第" + line + "行第" + column + "列");
        }
    }

    private void fillUsageCount(List<YaTemplateSchemaVo> voList) {
        for (YaTemplateSchemaVo vo : voList) {
            // 统计被引用的模板页面数量
            long count = templatePageService.count(
                new LambdaQueryWrapper<YaTemplatePage>().eq(YaTemplatePage::getTemplateSchemaId, vo.getId())
            );
            vo.setUsageCount(count);
        }
    }

    private LambdaQueryWrapper<YaTemplateSchema> buildWrapper(YaTemplateSchemaQueryDto q) {
        LambdaQueryWrapper<YaTemplateSchema> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.eq(q.getStatus() != null, YaTemplateSchema::getStatus, q.getStatus());
        qw.orderByDesc(YaTemplateSchema::getCreateAt);
        return qw;
    }
}
