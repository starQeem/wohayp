package com.starQeem.wohayp.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @Date: 2023/5/29 19:48
 * @author: Qeem
 */
@Data
public class File {
    @TableId
    private Long fileId;  //文件id
    private Long userId;  //用户id
    private String fileMd5; //文件MD5值
    private String filePid; //父级id
    private Long fileSize; //文件大小
    private String fileName; //文件名
    private String fileCover; //封面
    private String filePath; //文件路径
    private Integer folderType; //0:文件 1:目录
    private Integer fileCategory; //1:视频 2:音频 3:图片 4:文档 5:其它
    private Integer fileType; //文件类型 1:视频 2:音频 3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其它
    private Integer status; //状态
    private Integer delFlag; //0:删除 1:回收站 2:正常
    @TableField(exist = false)//标注为非数据库字段
    private String nickName;  //用户昵称
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date recoveryTime; //进入回收站时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; //创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateTime; //更新时间

}
