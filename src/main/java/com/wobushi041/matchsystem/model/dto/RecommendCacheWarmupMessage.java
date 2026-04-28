package com.wobushi041.matchsystem.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RecommendCacheWarmupMessage implements Serializable {

    private static final long serialVersionUID = 6419200407303929663L;

    /**
     * 长期任务标识，相同 userId/pageNum/pageSize 保持不变。
     * 由userId，pageNum，pageSize拼接而成
     */
    private String taskId;

    /**
     * 单次投递标识，方便追踪一条具体消息。
     */
    private String runId;

    private Long userId;

    private long pageNum;

    private long pageSize;

    private Long createTime;
}
