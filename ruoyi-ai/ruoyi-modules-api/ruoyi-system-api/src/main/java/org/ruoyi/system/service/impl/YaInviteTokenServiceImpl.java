package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaInvite;
import org.ruoyi.system.domain.YaInviteToken;
import org.ruoyi.system.domain.dto.YaInviteTokenDto;
import org.ruoyi.system.domain.dto.YaInviteTokenQueryDto;
import org.ruoyi.system.domain.vo.YaInviteTokenVo;
import org.ruoyi.system.mapper.YaInviteTokenMapper;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaInviteService;
import org.ruoyi.system.service.IYaInviteTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YaInviteTokenServiceImpl extends ServiceImpl<YaInviteTokenMapper, YaInviteToken> implements IYaInviteTokenService {

    @Autowired
    private IYaAlbumService yaAlbumService;
    @Autowired
    private IYaInviteService yaInviteService;

    @Override
    public TableDataInfo<YaInviteTokenVo> queryPage(YaInviteTokenQueryDto query, PageQuery pageQuery) {
        Page<YaInviteToken> page = this.page(pageQuery.build(), buildWrapper(query));
        return new TableDataInfo<>(BeanUtil.copyToList(page.getRecords(), YaInviteTokenVo.class), page.getTotal());
    }

    @Override
    public List<YaInviteTokenVo> queryList(YaInviteTokenQueryDto query) {
        return BeanUtil.copyToList(this.list(buildWrapper(query)), YaInviteTokenVo.class);
    }

    @Override
    public YaInviteTokenVo queryById(Integer id) {
        YaInviteToken entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaInviteTokenVo.class);
    }

    @Override
    public boolean insertByDto(YaInviteTokenDto dto) {
        YaInviteToken yaInviteToken = BeanUtil.toBean(dto, YaInviteToken.class);
        // 验证纪念册存在
        if(yaAlbumService.count(
                new LambdaQueryWrapper<YaAlbum>().eq(YaAlbum::getId, dto.getAlbumId())
        ) == 0){
            throw new RuntimeException("该纪念册不存在");
        }
        //验证邀请ID存在
        if(yaInviteService.count(
                new LambdaQueryWrapper<YaInvite>()
                        .eq(YaInvite::getId, dto.getInviteId())
        ) == 0){
            throw new RuntimeException("该邀请不存在");
        }
        yaInviteToken.setStatus(dto.getStatus()!=null?dto.getStatus():CommonConstants.IS_AVAILABLE);
        return this.save(yaInviteToken);
    }

    @Override
    public boolean updateByDto(YaInviteTokenDto dto) {
        return this.updateById(BeanUtil.toBean(dto, YaInviteToken.class));
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaInviteToken> buildWrapper(YaInviteTokenQueryDto q) {
        LambdaQueryWrapper<YaInviteToken> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.eq(q.getAlbumId() != null, YaInviteToken::getAlbumId, q.getAlbumId());
        qw.eq(q.getInviteId() != null, YaInviteToken::getInviteId, q.getInviteId());
        qw.eq(q.getStatus() != null, YaInviteToken::getStatus, q.getStatus());
        qw.orderByDesc(YaInviteToken::getCreateAt);
        return qw;
    }
}
