package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.catalina.security.SecurityUtil;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaTemplate;
import org.ruoyi.system.domain.dto.YaTemplateDto;
import org.ruoyi.system.domain.dto.YaTemplateQueryDto;
import org.ruoyi.system.domain.vo.YaTemplateVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.mapper.YaTemplateMapper;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class YaTemplateServiceImpl extends ServiceImpl<YaTemplateMapper, YaTemplate> implements IYaTemplateService {

    @Autowired
    private IYaAlbumService albumService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public TableDataInfo<YaTemplateVo> queryPage(YaTemplateQueryDto query, PageQuery pageQuery) {
        Page<YaTemplate> page = this.page(pageQuery.build(), buildWrapper(query));
        List<YaTemplateVo> voList = BeanUtil.copyToList(page.getRecords(), YaTemplateVo.class);
        fillUserNames(voList);
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
                throw new RuntimeException("模板 [id=" + templateId + "] 已被纪念册使用，无法删除");
            }
        }
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
}
