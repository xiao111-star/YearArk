package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.CommonConstants;
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

    @Override
    public TableDataInfo<YaTemplateSchemaVo> queryPage(YaTemplateSchemaQueryDto query, PageQuery pageQuery) {
        Page<YaTemplateSchema> page = this.page(pageQuery.build(), buildWrapper(query));
        return new TableDataInfo<>(BeanUtil.copyToList(page.getRecords(), YaTemplateSchemaVo.class), page.getTotal());
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
                throw new RuntimeException("Schema [id=" + schemaId + "] 已被模板页面使用，无法删除");
            }
        }
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaTemplateSchema> buildWrapper(YaTemplateSchemaQueryDto q) {
        LambdaQueryWrapper<YaTemplateSchema> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.eq(q.getStatus() != null, YaTemplateSchema::getStatus, q.getStatus());
        qw.orderByDesc(YaTemplateSchema::getCreateAt);
        return qw;
    }
}
