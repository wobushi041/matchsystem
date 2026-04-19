package com.wobushi041.matchsystem.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 添加队伍请求对象。
 * 用于接收前端传递的添加队伍的请求参数
 */
@Data
public class TeamAddRequest implements Serializable {

    private static final long serialVersionUID = 6191190645590093049L;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 用户id,Long类型
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;

}

