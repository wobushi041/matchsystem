package com.wobushi041.matchsystem.model.dto;

import com.wobushi041.matchsystem.model.request.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageRequest {
    /**
     * 队伍ID
     */
    private Long id;

    /**
     * 队伍列表
     */
    private List<Long> idList;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 创建队伍的用户ID
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

}
