package org.ruoyi.system.domain.vo;

import lombok.Data;

/**
 * 素材统计 VO
 *
 * @author YearArk
 */
@Data
public class MediaStatsVo {

    /** 图片数量 */
    private Long imageCount;

    /** 文字数量 */
    private Long textCount;
}
