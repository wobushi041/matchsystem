package com.wobushi041.matchsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.common.BaseResponse;
import com.wobushi041.matchsystem.common.ErrorCode;
import com.wobushi041.matchsystem.common.ResultUtils;
import com.wobushi041.matchsystem.exception.BusinessException;
import com.wobushi041.matchsystem.mapper.UserMapper;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.model.request.UserLoginRequest;
import com.wobushi041.matchsystem.model.request.UserRegisterRequest;
import com.wobushi041.matchsystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.wobushi041.matchsystem.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserMapper userMapper;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
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
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
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
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
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
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/update")
    public BaseResponse<Long> updateUser(@RequestBody User user, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        long result = userService.updateUser(user, loginUserFromRequest);
        return ResultUtils.success(result);
    }

    /**
     * 推荐用户列表接口。
     * 该接口不进行权限校验，向所有请求者开放。利用Redis缓存优化性能，如果缓存中存在用户数据，则直接返回，否则查询数据库,然后再写进缓存。
     * 使用分页查询来优化数据加载，只返回请求的页面数据。
     *
     * @param pageSize 每页显示的用户数，决定返回的用户列表长度。
     * @param pageNum  请求的页码，决定数据分页的起点。
     * @param request  HTTP请求对象，用于获取当前登录用户信息和未来可能的权限校验。
     * @return 返回经过脱敏处理的用户列表，保护用户隐私，包裹在统一响应结构中。
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        // 通过请求获取当前登录用户
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        // 格式化Redis键值，以用户ID作为唯一标识
        String redisKey = String.format("yupao:user:recommend:%s", loginUserFromRequest.getId());
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        // 尝试从Redis获取缓存的用户分页数据
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null) {
            // 如果缓存存在，直接返回缓存数据
            return ResultUtils.success(userPage);
        }
        // 缓存不存在时，创建查询包装器，默认不设置查询条件，查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 使用服务层的分页方法查询用户，自动处理数据分页
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        // 查询结果存入Redis缓存，并设置有效期为30000毫秒（30秒），以控制数据的新鲜度。
        // 异常处理用于捕获并记录设置缓存时可能出现的Redis操作错误。
        try {
            //TimeUnit.SECONDS（秒）
            //TimeUnit.MINUTES（分）
            //TimeUnit.HOURS（小时）
            valueOperations.set(redisKey, userPage, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        // 返回从数据库查询到的用户列表
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
    public BaseResponse<List<User>> searchUsers(@RequestParam(required = false) List<String> tagNameList) {
        //检查标签列表是否为空
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }
        List<User> userList = userService.searchUsersByTagId(tagNameList);
        return ResultUtils.success(userList);
    }

}
