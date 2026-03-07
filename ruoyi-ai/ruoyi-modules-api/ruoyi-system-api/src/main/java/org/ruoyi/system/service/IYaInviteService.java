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

    /**
     * 用户端创建邀请链接（自动生成邀请码、校验归属、计算过期时间）
     *
     * @param dto    邀请信息（albumId, accessCode, expireHours）
     * @param userId 当前用户ID
     * @return 是否成功
     */
    boolean createInvite(YaInviteDto dto, Integer userId);

    /**
     * 用户端禁用邀请链接（校验归属）
     *
     * @param id     邀请链接ID
     * @param userId 当前用户ID
     * @return 是否成功
     */
    boolean disableInvite(Integer id, Integer userId);
}
