package com.starQeem.wohayp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starQeem.wohayp.entity.pojo.File;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Date: 2023/5/29 19:57
 * @author: Qeem
 */
@Mapper
public interface fileMapper extends BaseMapper<File> {
    /**
     * 获取用户已经使用的空间
     *
     * @param userId 用户id
     * @return {@link Long}
     */
    Long getUseSpace(Long userId);

}
