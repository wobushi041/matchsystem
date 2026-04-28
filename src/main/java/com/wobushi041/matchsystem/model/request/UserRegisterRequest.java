package com.wobushi041.matchsystem.model.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.regex.Pattern;




/**
 * 用户注册请求体
 */
@Data
@NoArgsConstructor

/**
 * Spring 接收 @RequestBody JSON 并交给 Jackson 反序列化，Jackson 默认最容易识别、也最通用的方式就是：
 * 先调用无参构造
 * 再通过 setter 给每个字段赋值
 */
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;
    /**
     * 用户账号
     */
    @NotNull("账号不能为空")
    @Size(min = 4, message = "账号不能少于4位")
    private String userAccount;

    /**
     * 用户密码
     */
    @NotNull("密码不能为空")
    @Size(min = 8, message = "密码不能少于8位")
    private String userPassword;


    /**
     * 校验密码
     */
    @NotNull("验证密码不能为空")
    @Size(min = 8, message = "校验密码不能少于8位")
    private String checkPassword;

    /**
     * 星球编号
     */
    @NotNull("星球编号不能为空")
    @Size(max = 5, message = "星球编号过长")
    private String planetCode;
}
