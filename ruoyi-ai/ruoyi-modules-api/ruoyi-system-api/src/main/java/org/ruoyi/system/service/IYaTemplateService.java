package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaTemplate;
import org.ruoyi.system.domain.dto.YaTemplateDto;
import org.ruoyi.system.domain.dto.YaTemplateQueryDto;
import org.ruoyi.system.domain.vo.YaTemplateVo;

import java.util.List;

public interface IYaTemplateService extends IService<YaTemplate> {

    TableDataInfo<YaTemplateVo> queryPage(YaTemplateQueryDto query, PageQuery pageQuery);

    List<YaTemplateVo> queryList(YaTemplateQueryDto query);

    YaTemplateVo queryById(Integer id);

    boolean insertByDto(YaTemplateDto dto);

    boolean updateByDto(YaTemplateDto dto);

    boolean deleteByIds(List<Integer> ids);
}
