package com.starQeem.wohayp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.starQeem.wohayp.entity.dto.SessionShareDto;
import com.starQeem.wohayp.entity.pojo.share;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;

/**
 * @Date: 2023/6/4 10:09
 * @author: Qeem
 */
public interface shareService extends IService<share> {
    /**
     * 获取分享文件列表
     *
     * @param userId   用户id
     * @param pageNo   当前页码
     * @param pageSize 一页的数据条数
     * @return {@link PaginationResultVO}<{@link share}>
     */
    PaginationResultVO<share> pageFileList(Long userId, Integer pageNo, Integer pageSize);


    /**
     * 保存文件共享
     *
     * @param share 分享
     */
    void saveShare(share share);

    /**
     * 删除文件共享
     *
     * @param shareIds 共享id
     * @param userId   用户id
     */
    void deleteFileShareBath(String shareIds, Long userId);

    /**
     * 校验分享码
     *
     * @param shareId 共享id
     * @param code    提取码
     * @return {@link SessionShareDto}
     */
    SessionShareDto checkShareCode(String shareId, String code);
}
