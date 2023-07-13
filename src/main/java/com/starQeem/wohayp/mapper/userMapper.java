package com.starQeem.wohayp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starQeem.wohayp.entity.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Date: 2023/5/25 19:16
 * @author: Qeem
 */
@Mapper
public interface userMapper extends BaseMapper<User> {
    /**
     * 更新用户空间
     *
     * @param userId     用户id
     * @param useSpace   使用空间
     * @param totalSpace 总空间
     * @return {@link Integer}
     */
    Integer updateUserSpace(@Param("userId") Long userId,@Param("useSpace") Long useSpace,@Param("totalSpace") Long totalSpace);
}
