package com.wobushi041.matchsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wobushi041.matchsystem.model.domain.UserTeam;
import com.wobushi041.matchsystem.service.UserTeamService;
import com.wobushi041.matchsystem.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author 硫酸铜
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2026-04-15 13:22:03
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




