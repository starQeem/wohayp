package com.starQeem.wohayp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.starQeem.wohayp.entity.dto.SessionShareDto;
import com.starQeem.wohayp.entity.enums.ShareValidTypeEnums;
import com.starQeem.wohayp.entity.pojo.file;
import com.starQeem.wohayp.entity.pojo.share;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import com.starQeem.wohayp.exception.BusinessException;
import com.starQeem.wohayp.exception.ResponseCodeEnum;
import com.starQeem.wohayp.mapper.shareMapper;
import com.starQeem.wohayp.service.fileService;
import com.starQeem.wohayp.service.shareService;
import com.starQeem.wohayp.util.DateUtil;
import com.starQeem.wohayp.util.SnowflakeIdUtils;
import com.starQeem.wohayp.util.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

import static com.starQeem.wohayp.util.Constants.*;

/**
 * @Date: 2023/6/4 10:09
 * @author: Qeem
 */
@Service
public class shareServiceImpl extends ServiceImpl<shareMapper, share> implements shareService{
    @Resource
    private shareService shareService;
    @Resource
    private fileService fileService;
    /**
     * 获取分享文件列表
     *
     * @param userId   用户id
     * @param pageNo   当前页码
     * @param pageSize 一页的数据条数
     * @return {@link PaginationResultVO}<{@link share}>
     */
    @Override
    public PaginationResultVO<share> pageFileList(Long userId, Integer pageNo, Integer pageSize) {
        if (pageNo == null){
            pageNo = ONE;
        }
        if (pageSize == null){
            pageSize = PAGE_SIZE;
        }
        PageHelper.startPage(pageNo,pageSize);
        PageHelper.orderBy("share_time desc");
        //查询分享表
        QueryWrapper<share> shareQueryWrapper = new QueryWrapper<>();
        shareQueryWrapper
                .select("share_id","file_id","user_id","valid_type","expire_time","share_time","code","view")
                .eq("user_id",userId);
        List<share> shareList = shareService.getBaseMapper().selectList(shareQueryWrapper);
        //查询文件表
        QueryWrapper<file> fileQueryWrapper = new QueryWrapper<>();
        fileQueryWrapper.select("file_id","file_name","folder_type","file_type","file_cover")
                .eq("user_id",userId);
        List<file> fileList = fileService.getBaseMapper().selectList(fileQueryWrapper);
        //如果将文件表中的file_id与分享表的file_id相同,则将文件表的文件名和文件类型赋值给shareList
        fileList.forEach(file -> {
            shareList.stream()
                    .filter(share -> file.getFileId().equals(share.getFileId()))
                    .forEach(share -> {
                        share.setFileName(file.getFileName());
                        share.setFolderType(file.getFolderType());
                        share.setFileType(file.getFileType());
                        share.setFileCover(file.getFileCover());
                    });
        });
        PageInfo<share> pageInfo = new PageInfo<>(shareList);
        return new PaginationResultVO<>(Math.toIntExact(pageInfo.getTotal()), pageInfo.getPageSize(), pageInfo.getPageNum(), pageInfo.getPages(), shareList);
    }

    /**
     * 保存共享
     *
     * @param share 分享
     */
    @Override
    public void saveShare(share share) {
        ShareValidTypeEnums typeEnums = ShareValidTypeEnums.getByType(share.getValidType());
        if (typeEnums == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (ShareValidTypeEnums.FOREVER != typeEnums){
            share.setExpireTime(DateUtil.getAfterDate(typeEnums.getDays()));//保存失效时间
        }
        Date curDate = new Date();
        share.setShareTime(curDate);
        if (StringTools.isEmpty(share.getCode())){//判断是否有提取码
            //没有,则为系统生成,搞一个
            share.setCode(StringTools.getRandomString(LENGTH_5));
        }
        SnowflakeIdUtils idUtils = new SnowflakeIdUtils();
        share.setShareId(idUtils.nextId());
        shareService.save(share);
    }

    /**
     * 删除文件共享
     *
     * @param shareIds 共享id
     * @param userId   用户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBath(String shareIds, Long userId) {
        QueryWrapper<share> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId).last("and share_id in(" + shareIds + ")");
        int count = shareService.getBaseMapper().delete(queryWrapper);
        String[] strings = shareIds.split(",");
        if (count != strings.length){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    /**
     * 校验分享码
     *
     * @param shareId 共享id
     * @param code    提取码
     * @return {@link SessionShareDto}
     */
    @Override
    public SessionShareDto checkShareCode(String shareId, String code) {
        QueryWrapper<share> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("share_id",Long.valueOf(shareId));
        share share = shareService.getBaseMapper().selectOne(queryWrapper);
        if (share == null || share.getExpireTime() != null && new Date().after(share.getExpireTime())){ //判断分享链接是否存在,是否失效
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg()); //分享链接不存在或者已经失效
        }
        if (!share.getCode().equals(code)){
            throw new BusinessException("提取码错误!");
        }
        //更新浏览次数
        UpdateWrapper<share> updateWrapper = new UpdateWrapper<>();
        updateWrapper.setSql("view = view + 1").eq("share_id", shareId);
        shareService.update(updateWrapper);
        SessionShareDto shareSessionDto = new SessionShareDto();
        shareSessionDto.setShareUserId(Long.valueOf(shareId));
        shareSessionDto.setShareUserId(share.getUserId());
        shareSessionDto.setFileId(share.getFileId());
        shareSessionDto.setExpireTime(share.getExpireTime());
        return shareSessionDto;
    }
}
