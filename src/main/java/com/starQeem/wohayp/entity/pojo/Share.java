package com.starQeem.wohayp.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @Date: 2023/6/4 10:04
 * @author: Qeem
 */
@Data
public class Share {
    @TableId
    private Long shareId;  //分享id
    private Long fileId;  //文件id
    private Long userId;  //分享人id
    private Integer validType;   //有效期类型
    private String code;  //分享码
    private Integer view;  //浏览次数
    private String fileName; //文件名称
    @TableField(exist = false)//标注为非数据库字段
    private Integer folderType;
    @TableField(exist = false)//标注为非数据库字段
    private Integer fileType;
    @TableField(exist = false)//标注为非数据库字段
    private String fileCover;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;  //失效时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shareTime;  //分享时间

}
