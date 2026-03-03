package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbumPage;
import org.ruoyi.system.domain.dto.YaAlbumPageDto;
import org.ruoyi.system.domain.dto.YaAlbumPageQueryDto;
import org.ruoyi.system.domain.vo.YaAlbumPageVo;

import java.util.List;

public interface IYaAlbumPageService extends IService<YaAlbumPage> {

    TableDataInfo<YaAlbumPageVo> queryPage(YaAlbumPageQueryDto query, PageQuery pageQuery);

    List<YaAlbumPageVo> queryList(YaAlbumPageQueryDto query);

    YaAlbumPageVo queryById(Integer id);

    boolean insertByDto(YaAlbumPageDto dto);

    boolean updateByDto(YaAlbumPageDto dto);

    boolean deleteByIds(List<Integer> ids);
}
