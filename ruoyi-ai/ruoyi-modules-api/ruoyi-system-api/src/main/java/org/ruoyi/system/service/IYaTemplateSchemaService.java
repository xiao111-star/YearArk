package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.YaTemplateSchema;
import org.ruoyi.system.domain.dto.YaTemplateSchemaDto;
import org.ruoyi.system.domain.dto.YaTemplateSchemaQueryDto;
import org.ruoyi.system.domain.vo.YaTemplateSchemaVo;

import java.util.List;

public interface IYaTemplateSchemaService extends IService<YaTemplateSchema> {

    TableDataInfo<YaTemplateSchemaVo> queryPage(YaTemplateSchemaQueryDto query, PageQuery pageQuery);

    List<YaTemplateSchemaVo> queryList(YaTemplateSchemaQueryDto query);

    YaTemplateSchemaVo queryById(Integer id);

    boolean insertByDto(YaTemplateSchemaDto dto);

    boolean updateByDto(YaTemplateSchemaDto dto);

    boolean deleteByIds(List<Integer> ids);
}
