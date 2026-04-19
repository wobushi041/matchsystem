package com.wobushi041.matchsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.common.BaseResponse;
import com.wobushi041.matchsystem.common.ErrorCode;
import com.wobushi041.matchsystem.common.ResultUtils;
import com.wobushi041.matchsystem.exception.BusinessException;
import com.wobushi041.matchsystem.model.domain.Team;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.model.domain.UserTeam;
import com.wobushi041.matchsystem.model.dto.TeamQuery;
import com.wobushi041.matchsystem.model.request.TeamAddRequest;
import com.wobushi041.matchsystem.model.request.TeamJoinRequest;
import com.wobushi041.matchsystem.model.request.TeamQuitRequest;
import com.wobushi041.matchsystem.model.request.TeamUpdateRequest;
import com.wobushi041.matchsystem.model.vo.TeamUserVO;
import com.wobushi041.matchsystem.service.TeamService;
import com.wobushi041.matchsystem.service.UserService;
import com.wobushi041.matchsystem.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 处理队伍相关的增删改查请求
 *
 * @author 041
 */
@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 这里request不需要判空，因为 getLoginUserFromRequest 内部已经处理了 null 的情况并抛出异常
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        //将参数交给service层处理，返回队伍id
        long result = teamService.addTeam(team, loginUserFromRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody Long id, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        boolean result = teamService.deleteTeam(id, loginUserFromRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "插入失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        // 参数校验
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍更新信息不能为空");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未登录");
        }
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        // 执行更新操作
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUserFromRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(@RequestParam long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(team);

        }

    /**
     * 查询队伍列表接口。
     * 根据用户提供的查询条件，查询并返回符合条件的队伍列表。
     * 同时标注当前登录用户是否已加入这些队伍:取出查询的team的id，与用户加入的team的id进行比对，如果存在则说明已加入，contains方法
     * @param teamQuery 查询条件。
     * @param request HTTP请求对象，用于获取当前请求的用户信息。
     * @return 包含查询结果和加入状态的统一响应结构。
     * @throws BusinessException 当请求参数异常或查询过程中出现问题时抛出。
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(@ParameterObject TeamQuery teamQuery, HttpServletRequest request) {
        // 参数校验
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断当前用户是否为管理员
        boolean isAdmin = userService.isAdmin(request);
        // 调用队伍服务查询队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);
        if (teamList == null || CollectionUtils.isEmpty(teamList)) {
            return ResultUtils.success(new ArrayList<>());
        }
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        try {
            User loginUserFromRequest = userService.getLoginUserFromRequest(request);
            queryWrapper.in("teamId", teamIdList);
            queryWrapper.eq("userId", loginUserFromRequest.getId());
            List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
        }
        // 根据队伍ID，查询所有相关联的UserTeam记录，用于计算每个队伍的成员数量
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeamJoinList = userTeamService.list(userTeamJoinQueryWrapper);
        // 将队伍成员按队伍ID分组，便于计算每个队伍的成员数量
        Map<Long, List<UserTeam>> teamIdUserTeamMap = userTeamJoinList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team -> {
            // 设置每个队伍的成员数量
            team.setHasJoinNum(teamIdUserTeamMap.getOrDefault(team.getId(), new ArrayList<>()).size());
        });
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //构造条件
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(),teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page,queryWrapper);
        return ResultUtils.success(resultPage);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 这里request不需要判空，因为 getLoginUserFromRequest 内部已经处理了 null 的情况并抛出异常
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        //将参数交给service层处理
        boolean result = teamService.joinTeam(teamJoinRequest, loginUserFromRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest== null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求体不能为空");
        }
        // 这里request不需要判空，因为 getLoginUserFromRequest 内部已经处理了 null 的情况并抛出异常
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        //将参数交给service层处理
        boolean result = teamService.quitTeam(teamQuitRequest, loginUserFromRequest);
        return ResultUtils.success(result);
    }


    /**
     * 获取我创建的队伍
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUserFromRequest(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }


    /**
     * 获取我加入的队伍
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUserFromRequest(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        // 取出不重复的队伍 id
        // teamId userId
        // 1, 2
        // 1, 3
        // 2, 3
        // result
        // 1 => 2, 3
        // 2 => 3
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }


}
