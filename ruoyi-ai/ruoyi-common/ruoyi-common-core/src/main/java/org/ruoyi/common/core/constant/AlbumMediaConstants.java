package org.ruoyi.common.core.constant;

public interface AlbumMediaConstants {
    // 素材状态 - 待审核
    Integer STATUS_WAIT_AUDIT = 0;
    // 素材状态 - 审核中
    Integer STATUS_AUDITING = 1;
    // 素材状态 - 审核通过
    Integer STATUS_AUDIT_PASS = 2;
    // 素材状态 - 审核未通过
    Integer STATUS_AUDIT_NOT_PASS = -1;
    // 素材状态 - 已使用
    Integer STATUS_USED = 3;
}
