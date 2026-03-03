package org.ruoyi.system.controller.yearark;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.YaUserDto;
import org.ruoyi.system.domain.dto.YaUserQueryDto;
import org.ruoyi.system.domain.vo.YaUserVo;
import org.ruoyi.system.service.IYaUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端用户管理
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/yearark/user")
public class YaUserController extends BaseController {

    private final IYaUserService userService;

    /**
     * 分页查询用户列表
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return 用户分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<YaUserVo> page(YaUserQueryDto query, PageQuery pageQuery) {
        return userService.queryPage(query, pageQuery);
    }

    /**
     * 查询用户列表（不分页）
     *
     * @param query 查询条件
     * @return 用户列表
     */
    @GetMapping("/list")
    public R<List<YaUserVo>> list(YaUserQueryDto query) {
        return R.ok(userService.queryList(query));
    }

    /**
     * 获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public R<YaUserVo> info(@PathVariable Integer id) {
        return R.ok(userService.queryById(id));
    }

    /**
     * 新增用户
     *
     * @param dto 用户信息
     * @return 操作结果
     */
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaUserDto dto) {
        if(StrUtil.isBlank(dto.getPasswordHash())){
            return R.fail("请输入密码");
        }
        return toAjax(userService.insertByDto(dto));
    }

    /**
     * 修改用户
     *
     * @param dto 用户信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaUserDto dto) {
        return toAjax(userService.updateByDto(dto));
    }

    /**
     * 删除用户
     *
     * @param ids 用户ID列表
     * @return 操作结果
     */
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Integer> ids) {
        return toAjax(userService.deleteByIds(ids));
    }
}
