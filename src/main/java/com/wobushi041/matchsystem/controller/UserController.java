package com.wobushi041.matchsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.common.BaseResponse;
import com.wobushi041.matchsystem.common.ErrorCode;
import com.wobushi041.matchsystem.common.ResultUtils;
import com.wobushi041.matchsystem.exception.BusinessException;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.model.request.UserLoginRequest;
import com.wobushi041.matchsystem.model.request.UserRegisterRequest;
import com.wobushi041.matchsystem.service.RecommendCacheService;
import com.wobushi041.matchsystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.stream.Collectors;

import static com.wobushi041.matchsystem.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Validated
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private RecommendCacheService recommendCacheService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody @Validated UserRegisterRequest userRegisterRequest) {
        long result = userService.userRegister(userRegisterRequest.getUserAccount(), userRegisterRequest.getUserPassword(), userRegisterRequest.getCheckPassword(), userRegisterRequest.getPlanetCode());
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody @Validated UserLoginRequest userLoginRequest, HttpServletRequest request) {
        User user = userService.userLogin(userLoginRequest.getUserAccount(),userLoginRequest.getUserPassword(), request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }


    /**
     * 查询user，未分层
     *
     * @param username
     * @param request
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 删除用户信息
     *
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestParam @Min(value = 1,message = "id非法") long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/update")
    public BaseResponse<Long> updateUser(@RequestBody User user, HttpServletRequest request) {
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        long result = userService.updateUser(user, loginUserFromRequest);
        return ResultUtils.success(result);
    }

    /**
     * 推荐用户列表接口。
     * 该接口不进行权限校验，向所有请求者开放。利用Redis缓存优化性能，如果缓存中存在用户数据，则直接返回，否则查询数据库,然后再写进缓存。
     * 使用分页查询来优化数据加载，只返回请求的页面数据。
     * @param pageSize 每页显示的用户数，决定返回的用户列表长度。
     * @param pageNum  请求的页码，决定数据分页的起点。
     * @param request  HTTP请求对象，用于获取当前登录用户信息和未来可能的权限校验。
     * @return 返回经过脱敏处理的用户列表，保护用户隐私，包裹在统一响应结构中。
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        Page<User> userPage = recommendCacheService.getRecommendUsers(pageNum, pageSize,loginUserFromRequest.getId());
        return ResultUtils.success(userPage);
    }


    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "匹配人数必须在1到20之间");
        }
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        List<User> matchUsers = userService.matchUsers(num, loginUserFromRequest);
        return ResultUtils.success(matchUsers);

    }

    /**
     * required设置为 false 后，表示该参数是可选的。如果前端请求中没有携带这个参数，Spring 会将方法参数绑定为 null（对于对象类型）或默认值，而不会报错
     * @param tagNameList
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTagId(@RequestParam(required = false) List<String> tagNameList) {
        //检查标签列表是否为空
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }
        List<User> userList = userService.searchUsersByTagId(tagNameList);
        return ResultUtils.success(userList);
    }

}
