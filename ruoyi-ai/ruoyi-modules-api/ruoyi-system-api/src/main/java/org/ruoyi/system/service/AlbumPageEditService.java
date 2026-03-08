package org.ruoyi.system.service;

import org.ruoyi.system.domain.dto.PageUpdateDto;
import org.ruoyi.system.domain.vo.EditablePageVo;
import org.ruoyi.system.domain.vo.RenderedPageVo;

import java.util.List;
import java.util.Map;

/**
 * 纪念册页面编辑保存服务
 */
public interface AlbumPageEditService {

    /**
     * 获取纪念册所有页面的编辑数据（Data JSON + schemaContent + 渲染 HTML）
     */
    List<EditablePageVo> getEditData(Integer albumId);

    /**
     * 更新单页 Data JSON，校验归属 + schema，通过后重新渲染并返回渲染结果
     */
    RenderedPageVo updatePageData(Integer pageId, Integer userId, Map<String, Object> dataMap);

    /**
     * 批量更新多页 Data JSON，事务内执行，任一页校验失败则整体回滚
     */
    List<RenderedPageVo> batchUpdatePageData(List<PageUpdateDto> updates);
}
