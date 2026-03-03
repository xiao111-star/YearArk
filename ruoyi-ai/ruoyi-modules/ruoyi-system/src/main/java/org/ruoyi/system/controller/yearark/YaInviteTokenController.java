package org.ruoyi.system.controller.yearark;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.YaInviteTokenDto;
import org.ruoyi.system.domain.dto.YaInviteTokenQueryDto;
import org.ruoyi.system.domain.vo.YaInviteTokenVo;
import org.ruoyi.system.service.IYaInviteTokenService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 匿名Token管理
 *
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/yearark/invite-token")
public class YaInviteTokenController extends BaseController {

    private final IYaInviteTokenService tokenService;

    /**
     * 分页查询匿名上传者Token列表
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return Token分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<YaInviteTokenVo> page(YaInviteTokenQueryDto query, PageQuery pageQuery) {
        return tokenService.queryPage(query, pageQuery);
    }

    /**
     * 查询匿名上传者Token列表（不分页）
     *
     * @param query 查询条件
     * @return Token列表
     */
    @GetMapping("/list")
    public R<List<YaInviteTokenVo>> list(YaInviteTokenQueryDto query) {
        return R.ok(tokenService.queryList(query));
    }

    /**
     * 获取匿名上传者Token详情
     *
     * @param id Token ID
     * @return Token详情
     */
    @GetMapping("/{id}")
    public R<YaInviteTokenVo> info(@PathVariable Integer id) {
        return R.ok(tokenService.queryById(id));
    }

    /**
     * 新增匿名上传者Token
     *
     * @param dto Token信息
     * @return 操作结果
     */
    @PostMapping
    public R<Void> insert(@Validated @RequestBody YaInviteTokenDto dto) {
        return toAjax(tokenService.insertByDto(dto));
    }

    /**
     * 修改匿名上传者Token
     *
     * @param dto Token信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public R<Void> update(@Validated @RequestBody YaInviteTokenDto dto) {
        return toAjax(tokenService.updateByDto(dto));
    }

    /**
     * 删除匿名上传者Token
     *
     * @param ids Token ID列表
     * @return 操作结果
     */
    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Integer> ids) {
        return toAjax(tokenService.deleteByIds(ids));
    }
}
