package com.wobushi041.matchsystem.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户退出队伍请求体
 */
@Data
public class TeamQuitRequest implements Serializable {


    private static final long serialVersionUID = 6395006031284178770L;

    /**
     * id
     */
    private Long teamId;

}
