package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.dto.YaAlbumDto;
import org.ruoyi.system.domain.dto.YaAlbumQueryDto;
import org.ruoyi.system.domain.vo.YaAlbumVo;

import java.util.List;

public interface IYaAlbumService extends IService<YaAlbum> {

    TableDataInfo<YaAlbumVo> queryPage(YaAlbumQueryDto query, PageQuery pageQuery);

    List<YaAlbumVo> queryList(YaAlbumQueryDto query);

    YaAlbumVo queryById(Integer id);

    Integer insertByDto(YaAlbumDto dto);

    boolean updateByDto(YaAlbumDto dto);

    boolean deleteByIds(List<Integer> ids);

    /**
     * 校验纪念册归属（album.userId == userId），不通过则抛异常
     */
    void checkOwnership(Integer albumId, Integer userId);

    /**
     * 获取生成状态（复用 queryById，返回 YaAlbumVo）
     */
    YaAlbumVo getGenerationStatus(Integer albumId);
}
