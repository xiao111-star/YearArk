package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.AlbumMediaConstants;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbumMedia;
import org.ruoyi.system.domain.dto.YaAlbumMediaDto;
import org.ruoyi.system.domain.dto.YaAlbumMediaQueryDto;
import org.ruoyi.system.domain.vo.MediaStatsVo;
import org.ruoyi.system.domain.vo.SysOssVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;
import org.ruoyi.system.mapper.YaAlbumMediaMapper;
import org.ruoyi.system.service.ISysOssService;
import org.ruoyi.system.service.IYaAlbumMediaService;
import org.ruoyi.system.service.IYaAlbumService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class YaAlbumMediaServiceImpl extends ServiceImpl<YaAlbumMediaMapper, YaAlbumMedia> implements IYaAlbumMediaService {

    private final IYaAlbumService albumService;
    private final ISysOssService ossService;

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

    @Override
    public YaAlbumMediaVo uploadImage(Integer albumId, Integer userId, MultipartFile file) {
        // 校验文件非空
        if (ObjectUtil.isNull(file) || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }

        // 校验所有权
        albumService.checkOwnership(albumId, userId);

        // 校验 MIME 类型为 image/*
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ServiceException("仅支持上传图片文件");
        }

        // 上传到 OSS
        SysOssVo ossVo = ossService.upload(file);

        // 构建 media 记录：type=2, status=2, tokenId=null
        YaAlbumMedia media = new YaAlbumMedia();
        media.setAlbumId(albumId);
        media.setTokenId(null);
        media.setType(2);
        media.setContent(ossVo.getUrl());
        media.setStatus(2);
        media.setSize((double) file.getSize() / (1024 * 1024));
        media.setCreateAt(LocalDateTime.now());
        media.setUpdateAt(LocalDateTime.now());
        media.setIsDelete(CommonConstants.NOT_DELETE);

        // 自增 sort
        Integer maxSort = this.lambdaQuery()
            .eq(YaAlbumMedia::getAlbumId, albumId)
            .orderByDesc(YaAlbumMedia::getSort)
            .last("LIMIT 1")
            .oneOpt()
            .map(YaAlbumMedia::getSort)
            .orElse(0);
        media.setSort(maxSort + 1);

        this.save(media);

        return BeanUtil.copyProperties(media, YaAlbumMediaVo.class);
    }

    @Override
    public void deleteMediaByUser(Integer mediaId, Integer userId) {
        YaAlbumMedia media = this.getById(mediaId);
        if (media == null) {
            throw new ServiceException("素材不存在");
        }
        // 校验所有权（通过纪念册归属验证）
        albumService.checkOwnership(media.getAlbumId(), userId);
        this.removeById(mediaId);
    }
}
