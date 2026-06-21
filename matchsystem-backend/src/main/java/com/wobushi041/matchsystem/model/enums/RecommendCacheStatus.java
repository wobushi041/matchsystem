package com.wobushi041.matchsystem.model.enums;

public enum RecommendCacheStatus {
    //缺席
    ABSENT,
    //异步刷新
    REFRESH_AHEAD,
    //逻辑过期
    LOGIC_EXPIRED,
    //有效
    VALID
}
