package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaInvite;
import org.ruoyi.system.domain.dto.YaInviteDto;
import org.ruoyi.system.domain.dto.YaInviteQueryDto;
import org.ruoyi.system.domain.vo.YaInviteVo;
import org.ruoyi.system.mapper.YaInviteMapper;
import org.ruoyi.system.service.IYaInviteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YaInviteServiceImpl extends ServiceImpl<YaInviteMapper, YaInvite> implements IYaInviteService {

    @Override
    public TableDataInfo<YaInviteVo> queryPage(YaInviteQueryDto query, PageQuery pageQuery) {
        Page<YaInvite> page = this.page(pageQuery.build(), buildWrapper(query));
        return new TableDataInfo<>(BeanUtil.copyToList(page.getRecords(), YaInviteVo.class), page.getTotal());
    }

    @Override
    public List<YaInviteVo> queryList(YaInviteQueryDto query) {
        return BeanUtil.copyToList(this.list(buildWrapper(query)), YaInviteVo.class);
    }

    @Override
    public YaInviteVo queryById(Integer id) {
        YaInvite entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaInviteVo.class);
    }

    @Override
    public boolean insertByDto(YaInviteDto dto) {
        YaInvite yaInvite = BeanUtil.toBean(dto, YaInvite.class);
        yaInvite.setStatus(dto.getStatus()!=null?dto.getStatus():CommonConstants.IS_AVAILABLE);
        yaInvite.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(yaInvite);
    }

    @Override
    public boolean updateByDto(YaInviteDto dto) {
        return this.updateById(BeanUtil.toBean(dto, YaInvite.class));
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        for (Integer id : ids) {
            if (this.getById(id) == null) {
                throw new RuntimeException("邀请记录 [id=" + id + "] 不存在");
            }
        }
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaInvite> buildWrapper(YaInviteQueryDto q) {
        LambdaQueryWrapper<YaInvite> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.eq(q.getAlbumId() != null, YaInvite::getAlbumId, q.getAlbumId());
        qw.eq(q.getStatus() != null, YaInvite::getStatus, q.getStatus());
        qw.orderByDesc(YaInvite::getCreateAt);
        return qw;
    }
}
