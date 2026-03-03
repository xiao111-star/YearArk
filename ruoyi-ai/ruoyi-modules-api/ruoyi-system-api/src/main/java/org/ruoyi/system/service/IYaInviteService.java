package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaInvite;
import org.ruoyi.system.domain.dto.YaInviteDto;
import org.ruoyi.system.domain.dto.YaInviteQueryDto;
import org.ruoyi.system.domain.vo.YaInviteVo;

import java.util.List;

public interface IYaInviteService extends IService<YaInvite> {

    TableDataInfo<YaInviteVo> queryPage(YaInviteQueryDto query, PageQuery pageQuery);

    List<YaInviteVo> queryList(YaInviteQueryDto query);

    YaInviteVo queryById(Integer id);

    boolean insertByDto(YaInviteDto dto);

    boolean updateByDto(YaInviteDto dto);

    boolean deleteByIds(List<Integer> ids);
}
