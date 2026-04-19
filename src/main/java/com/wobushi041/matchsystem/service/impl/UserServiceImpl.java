package com.wobushi041.matchsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wobushi041.matchsystem.common.ErrorCode;
import com.wobushi041.matchsystem.contant.UserConstant;
import com.wobushi041.matchsystem.exception.BusinessException;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.service.UserService;
import com.wobushi041.matchsystem.mapper.UserMapper;
import com.wobushi041.matchsystem.utils.AlgorithmUtils;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.wobushi041.matchsystem.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";




    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    @Override
    public User getLoginUserFromRequest(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR,"用户未登录");
        }
        return (User) userObj;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 基于SQL查询，根据标签搜索用户
     *@param tagNamelist
     * @return
     */
    @Override
    public List<User> searchUsersByTagId(List<String> tagNamelist) {
        // 初始化查询包装器，并拼接标签的 AND 查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName : tagNamelist) {
            //模糊查询like
            // 使用标签列表的每一个标签项进行模糊查询
            // 从数据库中根据查询条件检索用户列表
            queryWrapper= queryWrapper.like("tags", tagName);

        }
        // 执行查询操作，并返回用户列表
        List<User> userList = userMapper.selectList(queryWrapper);
        // 利用流式处理返回安全用户对象列表
//        return userList.stream().map(user -> this.getSafetyUser(user)).collect(Collectors.toList());
          return userList.stream()
                  .map(this::getSafetyUser)
                  .collect(Collectors.toList());

    }

    /**
     * 基于内存查询，根据标签搜索用户
     * @param tagNameList 用户输入的标签列表，将在用户标签集合中进行匹配
     * @return 符合条件的用户列表
     * @throws BusinessException 当 `tagNameList` 为空或 `null` 时抛出参数错误异常
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        // 检查标签列表是否为空
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1. 查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        // 2. 在内存中过滤不符合标签要求的用户
        // 利用 Gson 将 JSON 字符串转换为 Set<String> 对象
        //parallelStream并发执行，区别于stream
        return userList.parallelStream()
                .filter(user -> {
                    // 获取用户的标签 JSON 字符串
                    String tagsStr = user.getTags();
                    // 如果标签字段为空，则过滤掉该用户
                    if (StringUtils.isBlank(tagsStr)) {
                        return false;
                    }
                    // 将 JSON 字符串解析为标签集合
                    Set<String> tmpTagNameList = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {}.getType());
                    // 如果用户的标签集合不包含所有目标标签，则过滤掉该用户
                    for (String tagName : tagNameList) {
                        if (!tmpTagNameList.contains(tagName)) {
                            return false;
                        }
                    }
                    return true;
                })
                // 将用户对象转换为安全用户对象
                .map(this::getSafetyUser)
                // 收集符合条件的用户列表
                .collect(Collectors.toList());

    }

    /**
     * 判断当前操作用户是否为管理员。
     * @param request HTTP请求对象，用于获取当前用户。
     * @return 如果当前用户为管理员，则返回true；否则返回false。
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
    try {
        User user = this.getLoginUserFromRequest(request);
        return user.getUserRole() == UserConstant.ADMIN_ROLE;
    }catch (BusinessException e){
        return false;
    }
    }

    /**
     * 判断当前操作用户是否为管理员。
     * @param loginUser 用户对象
     * @return 如果当前用户为管理员，则返回true；否则返回false。
     */
    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

/**
 * 匹配用户方法。
 * 根据当前用户的标签找出数据库中标签相似的用户。
 * 这个方法首先过滤出所有有标签的用户，然后使用编辑距离算法（Levenshtein distance）
 * 计算标签相似度，最后返回相似度最高的用户列表。
 * @param num 需要返回的用户数量，此数值应大于0且不超过20。
 * @param loginUser 当前登录的用户对象，用于从中提取标签进行比较。
 * @return 返回一个列表，包含与当前用户标签最相似的其他用户。
 */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        // 初始化查询条件，确保用户的标签不为空
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags"); // 确保查询的用户有标签信息
        queryWrapper.select("id", "tags"); // 仅选择id和tags字段进行查询，以减少数据量
        // 获取数据库中所有带标签的用户列表
        List<User> userList = this.list(queryWrapper);
        // 将当前用户的标签从JSON字符串转换为List
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>(){}.getType());
        // 准备用于存储用户与距离信息的列表
        List<Pair<User, Long>> list = new ArrayList<>();
        for (User user : userList) {
            String userTags = user.getTags();
            // 排除空标签以及当前用户自己
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>(){}.getType());
            // 过滤空标签列表
            if (CollectionUtils.isEmpty(userTagList)) {
                continue;
            }
            // 计算标签列表之间的编辑距离
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            //log.info("用户 {} 的标签: {}, 编辑距离: {}", user.getId(), userTagList, distance);
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离进行排序并获取距离最小的前num个用户
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted(Comparator.comparingLong(Pair::getSecond))  // 根据配对中的值（距离）进行排序
                .limit(num)  // 限制结果数量，只取距离最小的前num个用户
                .collect(Collectors.toList());  // 收集最终的配对列表
        // 提取最匹配的用户ID列表
        List<Long> userListVo = topUserPairList.stream()
                .map(pair -> pair.getFirst().getId())  // 从配对中提取用户ID
                .collect(Collectors.toList());
        // 根据ID重新查询用户信息，并进行脱敏处理
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userListVo);  // 使用用户ID过滤查询
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(this::getSafetyUser)  // 对用户信息进行脱敏处理
                .collect(Collectors.groupingBy(User::getId));  // 按用户ID进行分组
        // 按照 userListVo 的顺序组装最终的用户列表，保持相似度排序
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userListVo) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));  // 从映射中获取用户并添加到最终列表
        }
        return finalUserList;
    }
    /**
     * User user:前端传来的要更改的user；User loginUser：当前登录用户，
     * 要判断是不是管理员，不是的话判断是不是修改本人的字段(userId==loginUserId)
     * 最后调用userMapper.updataById(user)更新数据库
     * @param user
     * @param loginUser
     *  @return 更新操作影响的数据库记录数。通常返回1表示更新成功，返回0表示未进行更新。
     */
    @Override
    public int updateUser(User user, User loginUser) {
//        userId作用:校验，数据库查询和更新对应user
        long userId = user.getId();
        if (userId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(!isAdmin(loginUser)&&loginUser.getId()!=userId){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }


}



