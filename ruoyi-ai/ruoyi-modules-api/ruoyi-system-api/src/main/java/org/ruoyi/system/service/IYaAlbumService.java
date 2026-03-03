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

    boolean insertByDto(YaAlbumDto dto);

    boolean updateByDto(YaAlbumDto dto);

    boolean deleteByIds(List<Integer> ids);
}
