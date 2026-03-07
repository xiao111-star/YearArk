package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaUser;
import org.ruoyi.system.domain.dto.YaUserDto;
import org.ruoyi.system.domain.dto.YaUserQueryDto;
import org.ruoyi.system.domain.vo.YaUserVo;
import org.ruoyi.system.domain.vo.client.YaLoginVo;

import java.util.List;

public interface IYaUserService extends IService<YaUser> {

    TableDataInfo<YaUserVo> queryPage(YaUserQueryDto query, PageQuery pageQuery);

    List<YaUserVo> queryList(YaUserQueryDto query);

    YaUserVo queryById(Integer id);

    boolean insertByDto(YaUserDto dto);

    boolean updateByDto(YaUserDto dto);

    boolean deleteByIds(List<Integer> ids);

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param email    邮箱
     * @return 注册结果
     */
    R<Void> register(String username, String password, String email);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果（含 token）
     */
    R<YaLoginVo> login(String username, String password);

    /**
     * 用户退出登录
     *
     * @return 退出结果
     */
    R<Void> logout();
}

