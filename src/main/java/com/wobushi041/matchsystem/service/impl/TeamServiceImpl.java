package com.wobushi041.matchsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wobushi041.matchsystem.model.domain.Team;
import com.wobushi041.matchsystem.service.TeamService;
import com.wobushi041.matchsystem.mapper.TeamMapper;
import org.springframework.stereotype.Service;

/**
* @author 硫酸铜
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2026-04-15 13:21:52
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

}




