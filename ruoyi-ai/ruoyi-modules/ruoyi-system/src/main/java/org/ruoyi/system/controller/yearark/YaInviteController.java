package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.YaInviteDto;
import org.ruoyi.system.domain.dto.YaInviteQueryDto;
import org.ruoyi.system.domain.vo.YaInviteVo;
import org.ruoyi.system.service.IYaInviteService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 邀请链接
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/yearark/invite")
public class YaInviteController extends BaseController {

    private final IYaInviteService inviteService;

    /**
     * 分页查询邀请链接列表
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return 邀请链接分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<YaInviteVo> page(YaInviteQueryDto query, PageQuery pageQuery) {
        return inviteService.queryPage(query, pageQuery);
    }

    /**
     * 查询邀请链接列表（不分页）
     *
     * @param query 查询条件
     * @return 邀请链接列表
     */
    @GetMapping("/list")
    public R<List<YaInviteVo>> list(YaInviteQueryDto query) {
        return R.ok(inviteService.queryList(query));
    }

    /**
     * 获取邀请链接详情
     *
     * @param id 邀请链接ID
     * @return 邀请链接详情
     */
    @GetMapping("/{id}")
    public R<YaInviteVo> info(@PathVariable Integer id) {
        return R.ok(inviteService.queryById(id));
    }

    /**
     * 新增邀请链接
     *
     * @param dto 邀请链接信息
     * @return 操作结果
     */
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaInviteDto dto) {
        return toAjax(inviteService.insertByDto(dto));
    }

    /**
     * 修改邀请链接
     *
     * @param dto 邀请链接信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaInviteDto dto) {
        return toAjax(inviteService.updateByDto(dto));
    }

    /**
     * 删除邀请链接
     *
     * @param ids 邀请链接ID列表
     * @return 操作结果
     */
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Integer> ids) {
        return toAjax(inviteService.deleteByIds(ids));
    }
}
