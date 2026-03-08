package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.AlbumConstants;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaAlbumPage;
import org.ruoyi.system.domain.dto.YaAlbumDto;
import org.ruoyi.system.domain.dto.YaAlbumQueryDto;
import org.ruoyi.system.domain.vo.YaAlbumVo;import org.ruoyi.system.mapper.YaAlbumMapper;
import org.ruoyi.system.service.IYaAlbumPageService;
import org.ruoyi.system.service.IYaAlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YaAlbumServiceImpl extends ServiceImpl<YaAlbumMapper, YaAlbum> implements IYaAlbumService {
    
    @Autowired
    private IYaAlbumPageService albumPageService;

    @Override
    public TableDataInfo<YaAlbumVo> queryPage(YaAlbumQueryDto query, PageQuery pageQuery) {
        Page<YaAlbum> page = this.page(pageQuery.build(), buildWrapper(query));
        List<YaAlbumVo> voList = BeanUtil.copyToList(page.getRecords(), YaAlbumVo.class);
        return new TableDataInfo<>(voList, page.getTotal());
    }

    @Override
    public List<YaAlbumVo> queryList(YaAlbumQueryDto query) {
        List<YaAlbum> list = this.list(buildWrapper(query));
        return BeanUtil.copyToList(list, YaAlbumVo.class);
    }

    @Override
    public YaAlbumVo queryById(Integer id) {
        YaAlbum entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaAlbumVo.class);
    }

    @Override
    public boolean insertByDto(YaAlbumDto dto) {
        YaAlbum yaAlbum = BeanUtil.toBean(dto, YaAlbum.class);
        yaAlbum.setStatus(dto.getStatus()!=null? dto.getStatus(): CommonConstants.IS_AVAILABLE);
        yaAlbum.setIsPublic(AlbumConstants.IS_PUBLIC_NO);
        yaAlbum.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(yaAlbum);
    }

    @Override
    public boolean updateByDto(YaAlbumDto dto) {
        YaAlbum entity = BeanUtil.toBean(dto, YaAlbum.class);
        return this.updateById(entity);
    }

    // 删除接口，同时删除该纪念册下的所有纪念册单页
    @Override
    public boolean deleteByIds(List<Integer> ids) {
        for (Integer id : ids) {
            if (this.getById(id) == null) {
                throw new ServiceException("纪念册 [id=" + id + "] 不存在");
            }
        }
        // 先删除所有关联的 AlbumPage
        for (Integer albumId : ids) {
            LambdaQueryWrapper<YaAlbumPage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(YaAlbumPage::getAlbumId, albumId);
            albumPageService.remove(wrapper);
        }
        // 再删除 Album
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaAlbum> buildWrapper(YaAlbumQueryDto q) {
        LambdaQueryWrapper<YaAlbum> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.like(StrUtil.isNotBlank(q.getName()), YaAlbum::getName, q.getName());
        qw.eq(q.getUserId() != null, YaAlbum::getUserId, q.getUserId());
        qw.eq(q.getTemplateId() != null, YaAlbum::getTemplateId, q.getTemplateId());
        qw.eq(q.getStatus() != null, YaAlbum::getStatus, q.getStatus());
        qw.eq(q.getIsPublic() != null, YaAlbum::getIsPublic, q.getIsPublic());
        qw.orderByDesc(YaAlbum::getCreateAt);
        return qw;
    }

    @Override
    public void checkOwnership(Integer albumId, Integer userId) {
        YaAlbum album = this.getById(albumId);
        if (album == null || !album.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该纪念册");
        }
    }

    @Override
    public YaAlbumVo getGenerationStatus(Integer albumId) {
        YaAlbumVo vo = this.queryById(albumId);
        if (vo == null) throw new ServiceException("纪念册不存在");
        return vo;
    }
}
