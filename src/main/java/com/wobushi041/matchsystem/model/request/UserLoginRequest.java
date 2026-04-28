package com.wobushi041.matchsystem.model.request;

// [编程学习交流圈](https://www.code-nav.cn/) 快速入门编程不走弯路！30+ 原创学习路线和专栏、500+ 编程学习指南、1000+ 编程精华文章、20T+ 编程资源汇总

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户登录请求体
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户账号
     */
    @Size(min = 4, message = "账号不能少于4位")
    @NotBlank(message = "账号不能为空")
    private String userAccount;

    /**
     * 用户密码
     */
    @Size(min = 8, message = "密码不能少于8位")
    @NotBlank(message = "密码不能为空")
    private String userPassword;

    // https://www.code-nav.cn/
}
