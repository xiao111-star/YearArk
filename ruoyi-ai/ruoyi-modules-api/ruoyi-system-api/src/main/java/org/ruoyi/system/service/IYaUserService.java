package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaUser;
import org.ruoyi.system.domain.dto.ChangePasswordDto;
import org.ruoyi.system.domain.dto.UserProfileDto;
import org.ruoyi.system.domain.dto.YaUserDto;
import org.ruoyi.system.domain.dto.YaUserLoginDto;
import org.ruoyi.system.domain.dto.YaUserQueryDto;
import org.ruoyi.system.domain.dto.YaUserRegisterDto;
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
     * @param dto 注册信息（passwordHash 为前端 SHA-256 后的哈希值）
     * @return 注册结果
     */
    R<Void> register(YaUserRegisterDto dto);

    /**
     * 用户登录
     *
     * @param dto 登录信息（passwordHash 为前端 SHA-256 后的哈希值）
     * @return 登录结果（含 token）
     */
    R<YaLoginVo> login(YaUserLoginDto dto);

    /**
     * 用户退出登录
     *
     * @return 退出结果
     */
    R<Void> logout();

    /**
     * 更新个人资料
     */
    R<Void> updateProfile(Integer userId, UserProfileDto dto);

    /**
     * 更新头像
     */
    void updateAvatar(Integer userId, String avatarUrl);

    /**
     * 修改密码
     */
    R<Void> changePassword(Integer userId, ChangePasswordDto dto);
}

