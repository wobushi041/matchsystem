package com.wobushi041.matchsystem.service;

import com.wobushi041.matchsystem.service.impl.RecommendCacheServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendCacheServiceImplTest {

    @Test
    void buildRecommendCacheKey_shouldIncludeUserIdPageNumAndPageSize() {
        RecommendCacheServiceImpl service = new RecommendCacheServiceImpl();

        String redisKey = service.buildRecommendCacheKey(1L, 2L, 10L);

        assertEquals("user:recommend:1:2:10", redisKey);
    }
}
