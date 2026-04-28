package com.wobushi041.matchsystem.model.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.model.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendCacheValue implements Serializable {

    private static final long serialVersionUID = 8743980324169666342L;

    private Page<User> userPage;

    /**
     * 逻辑过期时间戳，单位毫秒。
     */
    private Long logicExpireTime;
}
