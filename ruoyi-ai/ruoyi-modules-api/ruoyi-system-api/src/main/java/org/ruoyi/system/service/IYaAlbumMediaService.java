package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbumMedia;
import org.ruoyi.system.domain.dto.YaAlbumMediaDto;
import org.ruoyi.system.domain.dto.YaAlbumMediaQueryDto;
import org.ruoyi.system.domain.vo.MediaStatsVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IYaAlbumMediaService extends IService<YaAlbumMedia> {

    TableDataInfo<YaAlbumMediaVo> queryPage(YaAlbumMediaQueryDto query, PageQuery pageQuery);

    List<YaAlbumMediaVo> queryList(YaAlbumMediaQueryDto query);

    YaAlbumMediaVo queryById(Integer id);

    boolean insertByDto(YaAlbumMediaDto dto);

    boolean updateByDto(YaAlbumMediaDto dto);

    boolean deleteByIds(List<Integer> ids);

    /**
     * 素材统计（图片数、文字数）
     */
    MediaStatsVo getStats(Integer albumId);

    /**
     * 获取纪念册下所有审核通过的图片列表（type=2, status=2），按 sort 升序返回
     */
    List<YaAlbumMediaVo> listUnusedImages(Integer albumId);

    /**
     * 登录用户上传图片到纪念册素材库
     *
     * @param albumId 纪念册ID
     * @param userId  当前登录用户ID
     * @param file    上传的图片文件
     * @return 新创建的素材VO
     */
    YaAlbumMediaVo uploadImage(Integer albumId, Integer userId, MultipartFile file);

    /**
     * 用户删除自己纪念册下的素材
     *
     * @param mediaId 素材ID
     * @param userId  当前登录用户ID
     */
    void deleteMediaByUser(Integer mediaId, Integer userId);
}
