package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbumPage;
import org.ruoyi.system.domain.YaTemplate;
import org.ruoyi.system.domain.YaTemplatePage;
import org.ruoyi.system.domain.YaTemplateSchema;
import org.ruoyi.system.domain.dto.YaTemplatePageDto;
import org.ruoyi.system.domain.dto.YaTemplatePageQueryDto;
import org.ruoyi.system.domain.vo.YaTemplatePageVo;
import org.ruoyi.system.mapper.YaTemplatePageMapper;
import org.ruoyi.system.service.IYaAlbumPageService;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.ruoyi.system.service.IYaTemplateSchemaService;
import org.ruoyi.system.service.IYaTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YaTemplatePageServiceImpl extends ServiceImpl<YaTemplatePageMapper, YaTemplatePage> implements IYaTemplatePageService {

    @Autowired
    private IYaAlbumPageService albumPageService;
    @Autowired
    private IYaTemplateService yaTemplateService;
    @Lazy
    @Autowired
    private IYaTemplateSchemaService yaTemplateSchemaService;

    @Override
    public TableDataInfo<YaTemplatePageVo> queryPage(YaTemplatePageQueryDto query, PageQuery pageQuery) {
        Page<YaTemplatePage> page = this.page(pageQuery.build(), buildWrapper(query));
        return new TableDataInfo<>(BeanUtil.copyToList(page.getRecords(), YaTemplatePageVo.class), page.getTotal());
    }

    @Override
    public List<YaTemplatePageVo> queryList(YaTemplatePageQueryDto query) {
        return BeanUtil.copyToList(this.list(buildWrapper(query)), YaTemplatePageVo.class);
    }

    @Override
    public YaTemplatePageVo queryById(Integer id) {
        YaTemplatePage entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaTemplatePageVo.class);
    }

    @Override
    public boolean insertByDto(YaTemplatePageDto dto) {
        //查询选择的模板是否存在
        if (yaTemplateService.count(
                new LambdaQueryWrapper<YaTemplate>()
                        .eq(YaTemplate::getId, dto.getTemplateId())
        ) == 0) {
            throw new RuntimeException("模板 [id=" + dto.getTemplateId() + "] 不存在");
        }
        //查询选择的schema是否存在
        if(yaTemplateSchemaService.count(
                new LambdaQueryWrapper<YaTemplateSchema>()
                        .eq(YaTemplateSchema::getId, dto.getTemplateSchemaId())
        ) == 0){
            throw new RuntimeException("模板 [id=" + dto.getTemplateId() + "] 的 Schema [id=" + dto.getTemplateSchemaId() + "] 不存在");
        }

        YaTemplatePage yaTemplatePage = BeanUtil.toBean(dto, YaTemplatePage.class);
        yaTemplatePage.setIsDelete(CommonConstants.NOT_DELETE);
        yaTemplatePage.setStatus(dto.getStatus()!=null? dto.getStatus(): CommonConstants.IS_AVAILABLE);
        Long userId = LoginHelper.getUserId();
        if (userId != null) {
            yaTemplatePage.setCreateBy(userId.intValue());
            yaTemplatePage.setUpdateBy(userId.intValue());
        }
        return this.save(yaTemplatePage);
    }

    @Override
    public boolean updateByDto(YaTemplatePageDto dto) {
        YaTemplatePage yaTemplatePage = BeanUtil.toBean(dto, YaTemplatePage.class);
        // 检查选择的模板是否存在
        if(ObjectUtil.isNotEmpty(dto.getTemplateId())){
            if(yaTemplateService.count(
                    new LambdaQueryWrapper<YaTemplate>()
                            .eq(YaTemplate::getId, dto.getTemplateId())
            ) == 0){
                throw new RuntimeException("模板 [id=" + dto.getTemplateId() + "] 不存在");
            }
        }
        // 检查选择的schema是否存在
        if(ObjectUtil.isNotEmpty(dto.getTemplateSchemaId())){
            if(yaTemplateSchemaService.count(
                    new LambdaQueryWrapper<YaTemplateSchema>()
                            .eq(YaTemplateSchema::getId, dto.getTemplateSchemaId())
            ) == 0){
                throw new RuntimeException("模板 [id=" + dto.getTemplateId() + "] 的 Schema [id=" + dto.getTemplateSchemaId() + "] 不存在");
            }
        }

        Long userId = LoginHelper.getUserId();
        if (userId != null) {
            yaTemplatePage.setUpdateBy(userId.intValue());
        }
        return this.updateById(yaTemplatePage);
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        // 检查是否有 AlbumPage 正在使用这些模板页
        for (Integer templatePageId : ids) {
            long count = albumPageService.count(
                new LambdaQueryWrapper<YaAlbumPage>().eq(YaAlbumPage::getTemplatePageId, templatePageId)
            );
            if (count > 0) {
                throw new RuntimeException("模板页 [id=" + templatePageId + "] 已被纪念册页面使用，无法删除");
            }
        }
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaTemplatePage> buildWrapper(YaTemplatePageQueryDto q) {
        LambdaQueryWrapper<YaTemplatePage> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.eq(q.getTemplateId() != null, YaTemplatePage::getTemplateId, q.getTemplateId());
        qw.eq(StrUtil.isNotBlank(q.getType()), YaTemplatePage::getType, q.getType());
        qw.orderByDesc(YaTemplatePage::getCreateAt);
        return qw;
    }
}
