package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaInviteToken;
import org.ruoyi.system.domain.dto.YaInviteTokenDto;
import org.ruoyi.system.domain.dto.YaInviteTokenQueryDto;
import org.ruoyi.system.domain.vo.YaInviteTokenVo;

import java.util.List;

public interface IYaInviteTokenService extends IService<YaInviteToken> {

    TableDataInfo<YaInviteTokenVo> queryPage(YaInviteTokenQueryDto query, PageQuery pageQuery);

    List<YaInviteTokenVo> queryList(YaInviteTokenQueryDto query);

    YaInviteTokenVo queryById(Integer id);

    boolean insertByDto(YaInviteTokenDto dto);

    boolean updateByDto(YaInviteTokenDto dto);

    boolean deleteByIds(List<Integer> ids);
}
