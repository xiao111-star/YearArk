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
import org.ruoyi.system.domain.YaAlbumMedia;
import org.ruoyi.system.domain.YaAlbumPage;
import org.ruoyi.system.domain.dto.YaAlbumDto;
import org.ruoyi.system.domain.dto.YaAlbumQueryDto;
import org.ruoyi.system.domain.vo.YaAlbumVo;import org.ruoyi.system.mapper.YaAlbumMapper;
import org.ruoyi.system.mapper.YaAlbumMediaMapper;
import org.ruoyi.system.service.IYaAlbumPageService;
import org.ruoyi.system.service.IYaAlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class YaAlbumServiceImpl extends ServiceImpl<YaAlbumMapper, YaAlbum> implements IYaAlbumService {
    
    @Autowired
    private IYaAlbumPageService albumPageService;

    @Autowired
    private YaAlbumMediaMapper albumMediaMapper;

    @Override
    public TableDataInfo<YaAlbumVo> queryPage(YaAlbumQueryDto query, PageQuery pageQuery) {
        Page<YaAlbum> page = this.page(pageQuery.build(), buildWrapper(query));
        List<YaAlbumVo> voList = BeanUtil.copyToList(page.getRecords(), YaAlbumVo.class);
        fillCoverUrls(voList);
        return new TableDataInfo<>(voList, page.getTotal());
    }

    @Override
    public List<YaAlbumVo> queryList(YaAlbumQueryDto query) {
        List<YaAlbum> list = this.list(buildWrapper(query));
        List<YaAlbumVo> voList = BeanUtil.copyToList(list, YaAlbumVo.class);
        fillCoverUrls(voList);
        return voList;
    }

    @Override
    public YaAlbumVo queryById(Integer id) {
        YaAlbum entity = this.getById(id);
        if (entity == null) return null;
        YaAlbumVo vo = BeanUtil.copyProperties(entity, YaAlbumVo.class);
        fillCoverUrls(Collections.singletonList(vo));
        return vo;
    }

    /**
     * 批量填充封面 URL：取素材库中首张审核通过的图片
     */
    private void fillCoverUrls(List<YaAlbumVo> voList) {
        if (voList == null || voList.isEmpty()) return;
        List<Integer> albumIds = voList.stream()
                .map(YaAlbumVo::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (albumIds.isEmpty()) return;

        LambdaQueryWrapper<YaAlbumMedia> mw = new LambdaQueryWrapper<>();
        mw.in(YaAlbumMedia::getAlbumId, albumIds)
                .eq(YaAlbumMedia::getType, 2)
                .orderByAsc(YaAlbumMedia::getSort);
        List<YaAlbumMedia> mediaList = albumMediaMapper.selectList(mw);

        Map<Integer, String> coverMap = new HashMap<>();
        for (YaAlbumMedia m : mediaList) {
            coverMap.putIfAbsent(m.getAlbumId(), m.getContent());
        }
        for (YaAlbumVo vo : voList) {
            if (vo.getCoverUrl() == null) {
                vo.setCoverUrl(coverMap.get(vo.getId()));
            }
        }
    }
    
    @Override
    public Integer insertByDto(YaAlbumDto dto) {
        YaAlbum yaAlbum = BeanUtil.toBean(dto, YaAlbum.class);
        yaAlbum.setStatus(dto.getStatus()!=null? dto.getStatus(): CommonConstants.IS_AVAILABLE);
        yaAlbum.setIsPublic(AlbumConstants.IS_PUBLIC_NO);
        yaAlbum.setIsDelete(CommonConstants.NOT_DELETE);
        this.save(yaAlbum);
        return yaAlbum.getId();
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

    @Override
    public boolean togglePublic(Integer albumId, Integer userId) {
        // 校验归属
        checkOwnership(albumId, userId);
        
        YaAlbum album = this.getById(albumId);
        if (album == null) {
            throw new ServiceException("纪念册不存在");
        }
        
        // 只有发布状态的纪念册才能放到首页
        if (album.getStatus() == null || !album.getStatus().equals(AlbumConstants.STATUS_PUBLISH)) {
            throw new ServiceException("只有发布状态的纪念册才能放到首页");
        }
        
        // 切换公开状态
        Integer newPublicStatus = AlbumConstants.IS_PUBLIC_NO.equals(album.getIsPublic()) 
            ? AlbumConstants.IS_PUBLIC_YES 
            : AlbumConstants.IS_PUBLIC_NO;
        
        album.setIsPublic(newPublicStatus);
        return this.updateById(album);
    }

    @Override
    public boolean publishAlbum(Integer albumId, Integer userId) {
        // 校验归属
        checkOwnership(albumId, userId);
        
        YaAlbum album = this.getById(albumId);
        if (album == null) {
            throw new ServiceException("纪念册不存在");
        }
        
        // 如果已经是发布状态，不需要重复发布
        if (AlbumConstants.STATUS_PUBLISH.equals(album.getStatus())) {
            throw new ServiceException("纪念册已经是发布状态");
        }
        
        // 更新为发布状态
        album.setStatus(AlbumConstants.STATUS_PUBLISH);
        return this.updateById(album);
    }
}
