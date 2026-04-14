package com.wobushi041.matchsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wobushi041.matchsystem.common.ErrorCode;
import com.wobushi041.matchsystem.exception.BusinessException;
import com.wobushi041.matchsystem.mapper.UserMapper;
import com.wobushi041.matchsystem.model.domain.Tag;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.service.TagService;
import com.wobushi041.matchsystem.mapper.TagMapper;
import com.wobushi041.matchsystem.service.UserService;
import lombok.Data;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 硫酸铜
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2026-04-11 15:17:13
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{
}




