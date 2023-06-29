package com.starQeem.wohayp.task;

/**
 * @Date: 2023/6/6 19:47
 * @author: Qeem
 * 定时任务:删除回收站过期的文件
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.starQeem.wohayp.entity.enums.FileDelFlagEnums;
import com.starQeem.wohayp.entity.pojo.file;
import com.starQeem.wohayp.service.fileService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;

import static com.starQeem.wohayp.util.Constants.RECYCLE_TTL;

@Component
public class FileCleanTask {
    @Resource
    private fileService fileService;
    @Scheduled(fixedDelay = 1000 * 60 * 10)  //每十分钟执行一次
    public void execute(){
        Date curDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(curDate);
        calendar.add(Calendar.DAY_OF_MONTH, - RECYCLE_TTL);
        Date tenDaysAgo = calendar.getTime();

        QueryWrapper<file> deleteQueryWrapper = new QueryWrapper<>();
        deleteQueryWrapper
                .eq("del_flag", FileDelFlagEnums.RECYCLE.getFlag())
                .lt("recovery_time", tenDaysAgo);
        fileService.getBaseMapper().delete(deleteQueryWrapper);

    }
}
