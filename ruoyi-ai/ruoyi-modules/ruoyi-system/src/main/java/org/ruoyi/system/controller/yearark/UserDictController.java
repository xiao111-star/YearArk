package org.ruoyi.system.controller.yearark;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.system.domain.vo.SysDictDataVo;
import org.ruoyi.system.service.ISysDictTypeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户端 - 字典数据查询（公开接口）
 *
 * @author YearArk
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/dict")
public class UserDictController {

    private final ISysDictTypeService dictTypeService;

    /**
     * 根据字典类型查询字典数据
     *
     * @param dictType 字典类型（如 sys_normal_disable）
     */
    @GetMapping("/type/{dictType}")
    public R<List<SysDictDataVo>> dictType(@PathVariable String dictType) {
        List<SysDictDataVo> data = dictTypeService.selectDictDataByType(dictType);
        if (ObjectUtil.isNull(data)) {
            data = new ArrayList<>();
        }
        return R.ok(data);
    }
}
