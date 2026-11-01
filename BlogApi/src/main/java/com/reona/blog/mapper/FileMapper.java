package com.reona.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reona.blog.entity.Files;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper extends BaseMapper<Files> {
}
