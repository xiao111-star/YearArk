package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaUser;
import org.ruoyi.system.domain.dto.YaUserDto;
import org.ruoyi.system.domain.dto.YaUserQueryDto;
import org.ruoyi.system.domain.vo.YaUserVo;
import org.ruoyi.system.mapper.YaUserMapper;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class YaUserServiceImpl extends ServiceImpl<YaUserMapper, YaUser> implements IYaUserService {

    @Autowired
    private IYaAlbumService yaAlbumService;

    @Override
    public TableDataInfo<YaUserVo> queryPage(YaUserQueryDto query, PageQuery pageQuery) {
        Page<YaUser> page = this.page(pageQuery.build(), buildWrapper(query));
        List<YaUserVo> voList = BeanUtil.copyToList(page.getRecords(), YaUserVo.class);
        return new TableDataInfo<>(voList, page.getTotal());
    }

    @Override
    public List<YaUserVo> queryList(YaUserQueryDto query) {
        return BeanUtil.copyToList(this.list(buildWrapper(query)), YaUserVo.class);
    }

    @Override
    public YaUserVo queryById(Integer id) {
        YaUser entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaUserVo.class);
    }

    @Override
    public boolean insertByDto(YaUserDto dto) {
        YaUser yaUser = BeanUtil.toBean(dto, YaUser.class);
        yaUser.setStatus(dto.getStatus()!=null?dto.getStatus():CommonConstants.IS_AVAILABLE);
        yaUser.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(yaUser);
    }

    @Override
    public boolean updateByDto(YaUserDto dto) {
        return this.updateById(BeanUtil.toBean(dto, YaUser.class));
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        ids.forEach(id -> {
            LambdaQueryWrapper<YaAlbum> qw = new LambdaQueryWrapper<>();
            qw.eq(YaAlbum::getUserId, id);
            List<YaAlbum> list = yaAlbumService.list(qw);
            if (!list.isEmpty()) {
                yaAlbumService.deleteByIds(list.stream().map(YaAlbum::getId).toList());
            }
        });
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaUser> buildWrapper(YaUserQueryDto q) {
        LambdaQueryWrapper<YaUser> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.like(StrUtil.isNotBlank(q.getUsername()), YaUser::getUsername, q.getUsername());
        qw.like(StrUtil.isNotBlank(q.getEmail()), YaUser::getEmail, q.getEmail());
        qw.eq(q.getStatus() != null, YaUser::getStatus, q.getStatus());
        qw.orderByDesc(YaUser::getCreateAt);
        return qw;
    }
}
