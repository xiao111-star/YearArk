package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaTemplate;
import org.ruoyi.system.domain.YaTemplatePage;
import org.ruoyi.system.domain.dto.YaTemplateDto;
import org.ruoyi.system.domain.dto.YaTemplateQueryDto;
import org.ruoyi.system.domain.vo.YaTemplateVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.mapper.YaTemplateMapper;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.ruoyi.system.service.IYaTemplateService;
import org.ruoyi.system.service.ISysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YaTemplateServiceImpl extends ServiceImpl<YaTemplateMapper, YaTemplate> implements IYaTemplateService {

    @Autowired
    private IYaAlbumService albumService;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private ISysDictDataService dictDataService;
    @Lazy
    @Autowired
    private IYaTemplatePageService yaTemplatePageService;

    @Override
    public TableDataInfo<YaTemplateVo> queryPage(YaTemplateQueryDto query, PageQuery pageQuery) {
        Page<YaTemplate> page = this.page(pageQuery.build(), buildWrapper(query));
        List<YaTemplateVo> voList = BeanUtil.copyToList(page.getRecords(), YaTemplateVo.class);
        fillUserNames(voList);
        fillAlbumCountAndTypeName(voList);
        return new TableDataInfo<>(voList, page.getTotal());
    }

    @Override
    public List<YaTemplateVo> queryList(YaTemplateQueryDto query) {
        List<YaTemplateVo> voList = BeanUtil.copyToList(this.list(buildWrapper(query)), YaTemplateVo.class);
        fillUserNames(voList);
        return voList;
    }

    @Override
    public YaTemplateVo queryById(Integer id) {
        YaTemplate entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        YaTemplateVo vo = BeanUtil.copyProperties(entity, YaTemplateVo.class);
        fillUserNames(List.of(vo));
        fillAlbumCountAndTypeName(List.of(vo));
        return vo;
    }

    @Override
    public boolean insertByDto(YaTemplateDto dto) {
        YaTemplate yaTemplate = BeanUtil.toBean(dto, YaTemplate.class);
        yaTemplate.setStatus(dto.getStatus()!=null?dto.getStatus():CommonConstants.IS_AVAILABLE);
        Long userId = LoginHelper.getUserId();
        if (userId != null) {
            yaTemplate.setCreateBy(userId.intValue());
            yaTemplate.setUpdateBy(userId.intValue());
        }
        yaTemplate.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(yaTemplate);
    }

    @Override
    public boolean updateByDto(YaTemplateDto dto) {
        if (ObjectUtil.isEmpty(dto.getId())) {
            throw new ServiceException("请选择要修改的模板");
        }
        YaTemplate yaTemplate = BeanUtil.toBean(dto, YaTemplate.class);
        Long userId = LoginHelper.getUserId();
        if (userId != null) {
            yaTemplate.setUpdateBy(userId.intValue());
        }
        yaTemplate.setIsDelete(CommonConstants.NOT_DELETE);
        return this.updateById(yaTemplate);
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        // 检查是否有 Album 正在使用这些模板
        for (Integer templateId : ids) {
            long count = albumService.count(
                new LambdaQueryWrapper<YaAlbum>().eq(YaAlbum::getTemplateId, templateId)
            );
            if (count > 0) {
                throw new ServiceException("模板 [id=" + templateId + "] 已被纪念册使用，无法删除");
            }
        }
        // 删除模板的时候 一并删除模板页
        ids.forEach(templateId ->
                yaTemplatePageService.remove(new LambdaQueryWrapper<YaTemplatePage>()
                        .eq(YaTemplatePage::getTemplateId, templateId))
        );
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaTemplate> buildWrapper(YaTemplateQueryDto q) {
        LambdaQueryWrapper<YaTemplate> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.like(StrUtil.isNotBlank(q.getName()), YaTemplate::getName, q.getName());
        qw.eq(q.getType() != null, YaTemplate::getType, q.getType());
        qw.eq(q.getStatus() != null, YaTemplate::getStatus, q.getStatus());
        qw.orderByDesc(YaTemplate::getCreateAt);
        return qw;
    }

    /**
     * 填充创建人和更新者的用户名
     */
    private void fillUserNames(List<YaTemplateVo> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        
        for (YaTemplateVo vo : voList) {
            if (vo.getCreateBy() != null) {
                SysUserVo createUser = sysUserMapper.selectUserById(vo.getCreateBy().longValue());
                if (createUser != null) {
                    vo.setCreateByName(createUser.getNickName());
                }
            }
            if (vo.getUpdateBy() != null) {
                SysUserVo updateUser = sysUserMapper.selectUserById(vo.getUpdateBy().longValue());
                if (updateUser != null) {
                    vo.setUpdateByName(updateUser.getNickName());
                }
            }
        }
    }

    /**
     * 填充纪念册使用数量和类型中文名称
     */
    private void fillAlbumCountAndTypeName(List<YaTemplateVo> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }

        for (YaTemplateVo vo : voList) {
            // 统计纪念册使用数量（@TableLogic 自动过滤 is_delete != 0 的记录）
            long count = albumService.count(
                new LambdaQueryWrapper<YaAlbum>().eq(YaAlbum::getTemplateId, vo.getId())
            );
            vo.setAlbumCount(count);

            // 字典翻译 type -> typeName
            if (vo.getType() != null) {
                try {
                    String label = dictDataService.selectDictLabel("ya_template_type", String.valueOf(vo.getType()));
                    vo.setTypeName(label);
                } catch (Exception e) {
                    // 字典未配置时不影响正常返回
                    vo.setTypeName(null);
                }
            }
        }
    }
}
