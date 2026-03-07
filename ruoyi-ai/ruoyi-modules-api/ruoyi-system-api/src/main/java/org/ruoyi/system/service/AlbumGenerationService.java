package org.ruoyi.system.service;

import org.ruoyi.common.core.domain.R;

/**
 * 纪念册生成服务
 *
 * @author YearArk
 */
public interface AlbumGenerationService {

    /**
     * 生成纪念册
     * <p>
     * 1. 验证纪念册已关联模板且有素材
     * 2. 读取模板页和 Schema
     * 3. 收集素材（图片+文字），按 sort 排序
     * 4. 按 Schema 的 imageCount/textCount 分配素材到模板页
     * 5. 生成 Data JSON
     * 6. 创建 ya_album_page 记录
     * 7. 更新 ya_album.status = 1
     *
     * @param albumId 纪念册ID
     * @return 操作结果
     */
    R<Void> generate(Integer albumId);
}
