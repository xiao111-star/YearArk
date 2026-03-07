package org.ruoyi.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaUser;
import org.ruoyi.system.domain.dto.YaUserDto;
import org.ruoyi.system.domain.dto.YaUserQueryDto;
import org.ruoyi.system.domain.model.YaLoginUser;
import org.ruoyi.system.domain.vo.YaUserVo;
import org.ruoyi.system.domain.vo.client.YaLoginVo;
import org.ruoyi.system.mapper.YaUserMapper;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaUserService;
import org.ruoyi.system.util.StpUserUtil;
import org.ruoyi.system.util.YaLoginHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YaUserServiceImpl extends ServiceImpl<YaUserMapper, YaUser> implements IYaUserService {

    @Autowired
    private IYaAlbumService yaAlbumService;

    @Override
    public TableDataInfo<YaUserVo> queryPage(YaUserQueryDto query, PageQuery pageQuery) {
        Page<YaUser> page = this.page(pageQuery.build(), buildWrapper(query));
        List<YaUserVo> voList = BeanUtil.copyToList(page.getRecords(), YaUserVo.class);
        return new TableDataInfo<>(voList, page.getTotal());
    }

    @Override
    public List<YaUserVo> queryList(YaUserQueryDto query) {
        return BeanUtil.copyToList(this.list(buildWrapper(query)), YaUserVo.class);
    }

    @Override
    public YaUserVo queryById(Integer id) {
        YaUser entity = this.getById(id);
        return entity == null ? null : BeanUtil.copyProperties(entity, YaUserVo.class);
    }

    @Override
    public boolean insertByDto(YaUserDto dto) {
        YaUser yaUser = BeanUtil.toBean(dto, YaUser.class);
        yaUser.setStatus(dto.getStatus()!=null?dto.getStatus():CommonConstants.IS_AVAILABLE);
        yaUser.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(yaUser);
    }

    @Override
    public boolean updateByDto(YaUserDto dto) {
        return this.updateById(BeanUtil.toBean(dto, YaUser.class));
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        ids.forEach(id -> {
            LambdaQueryWrapper<YaAlbum> qw = new LambdaQueryWrapper<>();
            qw.eq(YaAlbum::getUserId, id);
            List<YaAlbum> list = yaAlbumService.list(qw);
            if (!list.isEmpty()) {
                yaAlbumService.deleteByIds(list.stream().map(YaAlbum::getId).toList());
            }
        });
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<YaUser> buildWrapper(YaUserQueryDto q) {
        LambdaQueryWrapper<YaUser> qw = new LambdaQueryWrapper<>();
        if (q == null) return qw;
        qw.like(StrUtil.isNotBlank(q.getUsername()), YaUser::getUsername, q.getUsername());
        qw.like(StrUtil.isNotBlank(q.getEmail()), YaUser::getEmail, q.getEmail());
        qw.eq(q.getStatus() != null, YaUser::getStatus, q.getStatus());
        qw.orderByDesc(YaUser::getCreateAt);
        return qw;
    }

    @Override
    public R<Void> register(String username, String password, String email) {

        // Service 层校验用户名唯一
        long count = this.count(
            new LambdaQueryWrapper<YaUser>().eq(YaUser::getUsername, username)
        );
        if (count > 0) {
            return R.fail("用户名已存在");
        }

        // BCrypt 加密密码并保存
        YaUser yaUser = new YaUser();
        yaUser.setUsername(username);
        yaUser.setPasswordHash(BCrypt.hashpw(password));
        yaUser.setEmail(email);
        yaUser.setStatus(CommonConstants.IS_AVAILABLE);
        yaUser.setIsDelete(CommonConstants.NOT_DELETE);
        this.save(yaUser);

        return R.ok("注册成功");
    }

    @Override
    public R<YaLoginVo> login(String username, String password) {
        // 查询用户
        YaUser yaUser = this.getOne(
            new LambdaQueryWrapper<YaUser>()
                .eq(YaUser::getUsername, username)
                .eq(YaUser::getStatus, CommonConstants.IS_AVAILABLE)
        );
        if (yaUser == null) {
            return R.fail("用户名或密码错误");
        }

        // BCrypt 验证密码
        if (!BCrypt.checkpw(password, yaUser.getPasswordHash())) {
            return R.fail("用户名或密码错误");
        }

        // 构建 YaLoginUser 并登录
        YaLoginUser loginUser = new YaLoginUser();
        loginUser.setUserId(yaUser.getId());
        loginUser.setUsername(yaUser.getUsername());
        YaLoginHelper.login(loginUser);

        // 构建返回 VO
        YaLoginVo loginVo = new YaLoginVo();
        loginVo.setToken(StpUserUtil.getTokenValue());
        loginVo.setUserId(yaUser.getId());
        loginVo.setUsername(yaUser.getUsername());

        return R.ok(loginVo);
    }

    @Override
    public R<Void> logout() {
        try {
            StpUserUtil.logout();
            return R.ok("退出成功");
        } catch (Exception e) {
            return R.fail("退出失败");
        }
    }
}
