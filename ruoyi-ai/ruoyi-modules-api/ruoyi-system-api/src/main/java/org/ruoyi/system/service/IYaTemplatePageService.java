package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaTemplatePage;
import org.ruoyi.system.domain.dto.YaTemplatePageDto;
import org.ruoyi.system.domain.dto.YaTemplatePageQueryDto;
import org.ruoyi.system.domain.vo.YaTemplatePageVo;

import java.util.List;

public interface IYaTemplatePageService extends IService<YaTemplatePage> {

    TableDataInfo<YaTemplatePageVo> queryPage(YaTemplatePageQueryDto query, PageQuery pageQuery);

    List<YaTemplatePageVo> queryList(YaTemplatePageQueryDto query);

    YaTemplatePageVo queryById(Integer id);

    boolean insertByDto(YaTemplatePageDto dto);

    boolean updateByDto(YaTemplatePageDto dto);

    boolean deleteByIds(List<Integer> ids);
}
