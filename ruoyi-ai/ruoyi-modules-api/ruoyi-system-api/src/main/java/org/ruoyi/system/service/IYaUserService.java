package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaUser;
import org.ruoyi.system.domain.dto.YaUserDto;
import org.ruoyi.system.domain.dto.YaUserQueryDto;
import org.ruoyi.system.domain.vo.YaUserVo;

import java.util.List;

public interface IYaUserService extends IService<YaUser> {

    TableDataInfo<YaUserVo> queryPage(YaUserQueryDto query, PageQuery pageQuery);

    List<YaUserVo> queryList(YaUserQueryDto query);

    YaUserVo queryById(Integer id);

    boolean insertByDto(YaUserDto dto);

    boolean updateByDto(YaUserDto dto);

    boolean deleteByIds(List<Integer> ids);
}
