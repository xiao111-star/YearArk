package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbumMedia;
import org.ruoyi.system.domain.dto.YaAlbumMediaDto;
import org.ruoyi.system.domain.dto.YaAlbumMediaQueryDto;
import org.ruoyi.system.domain.vo.MediaStatsVo;
import org.ruoyi.system.domain.vo.YaAlbumMediaVo;

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
     *
     * @param albumId 纪念册ID
     * @return 统计信息
     */
    MediaStatsVo getStats(Integer albumId);
}
