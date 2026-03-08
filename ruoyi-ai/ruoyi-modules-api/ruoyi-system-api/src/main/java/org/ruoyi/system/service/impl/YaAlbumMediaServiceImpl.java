package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.AlbumMediaConstants;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbumMedia;
import org.ruoyi.system.domain.dto.YaAlbumMediaDto;
import org.ruoyi.system.domain.dto.YaAlbumMediaQueryDto;
import org.ruoyi.system.domain.vo.MediaStatsVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;
import org.ruoyi.system.mapper.YaAlbumMediaMapper;
import org.ruoyi.system.service.IYaAlbumMediaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class YaAlbumMediaServiceImpl extends ServiceImpl<YaAlbumMediaMapper, YaAlbumMedia> implements IYaAlbumMediaService {

    @Override
    public TableDataInfo<YaAlbumMediaVo> queryPage(YaAlbumMediaQueryDto query, PageQuery pageQuery) {
        Page<YaAlbumMedia> page = this.page(pageQuery.build(), buildWrapper(query));
        return new TableDataInfo<>(BeanUtil.copyToList(page.getRecords(), YaAlbumMediaVo.class), page.getTotal());
    }

    @Override
    public List<YaAlbumMediaVo> queryList(YaAlbumMediaQueryDto query) {
        return BeanUtil.copyToList(this.list(buildWrapper(query)), YaAlbumMediaVo.class);
    }

    @Override
    public YaAlbumMediaVo queryById(Integer id) {
        YaAlbumMedia entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaAlbumMediaVo.class);
    }

    @Override
    public boolean insertByDto(YaAlbumMediaDto dto) {
        YaAlbumMedia yaAlbumMedia = BeanUtil.toBean(dto, YaAlbumMedia.class);
        yaAlbumMedia.setIsDelete(CommonConstants.NOT_DELETE);
        yaAlbumMedia.setStatus(dto.getStatus() != null ? dto.getStatus() : AlbumMediaConstants.STATUS_WAIT_AUDIT);
        // 按 album_id 自增 sort
        Integer maxSort = this.lambdaQuery()
                .eq(YaAlbumMedia::getAlbumId, dto.getAlbumId())
                .orderByDesc(YaAlbumMedia::getSort)
                .last("LIMIT 1")
                .oneOpt()
                .map(YaAlbumMedia::getSort)
                .orElse(0);
        yaAlbumMedia.setSort(maxSort + 1);
        return this.save(yaAlbumMedia);
    }

    @Override
    public boolean updateByDto(YaAlbumMediaDto dto) {
        return this.updateById(BeanUtil.toBean(dto, YaAlbumMedia.class));
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        for(Integer id: ids){
            if(Objects.equals(this.queryById(id).getStatus(), AlbumMediaConstants.STATUS_USED)) {
                throw new RuntimeException("存在已使用的素材，无法删除");
            }
        }
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaAlbumMedia> buildWrapper(YaAlbumMediaQueryDto q) {
        LambdaQueryWrapper<YaAlbumMedia> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.eq(q.getAlbumId() != null, YaAlbumMedia::getAlbumId, q.getAlbumId());
        qw.eq(q.getType() != null, YaAlbumMedia::getType, q.getType());
        qw.eq(q.getStatus() != null, YaAlbumMedia::getStatus, q.getStatus());
        qw.like(StrUtil.isNotBlank(q.getTags()), YaAlbumMedia::getTags, q.getTags());
        qw.orderByAsc(YaAlbumMedia::getSort);
        return qw;
    }

    @Override
    public MediaStatsVo getStats(Integer albumId) {
        long imageCount = this.count(
            new LambdaQueryWrapper<YaAlbumMedia>()
                .eq(YaAlbumMedia::getAlbumId, albumId)
                .eq(YaAlbumMedia::getType, 2)
        );
        long textCount = this.count(
            new LambdaQueryWrapper<YaAlbumMedia>()
                .eq(YaAlbumMedia::getAlbumId, albumId)
                .eq(YaAlbumMedia::getType, 1)
        );

        MediaStatsVo stats = new MediaStatsVo();
        stats.setImageCount(imageCount);
        stats.setTextCount(textCount);
        return stats;
    }

    @Override
    public List<YaAlbumMediaVo> listUnusedImages(Integer albumId) {
        return BeanUtil.copyToList(
            this.lambdaQuery()
                .eq(YaAlbumMedia::getAlbumId, albumId)
                .eq(YaAlbumMedia::getType, 2)
                .eq(YaAlbumMedia::getStatus, 2)
                .orderByAsc(YaAlbumMedia::getSort)
                .list(),
            YaAlbumMediaVo.class
        );
    }
}
