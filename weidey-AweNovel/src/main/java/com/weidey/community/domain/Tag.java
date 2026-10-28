package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 标签 gal_tag
 *
 * @author weidey
 */
@Data
@TableName("gal_tag")
public class Tag {

    /** 标签ID */
    @TableId(value = "tag_id", type = IdType.AUTO)
    private Long tagId;

    /** 标签名称 */
    private String name;

    /** 标签类型（theme题材/play玩法/type类型） */
    private String type;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /** 关联游戏数（非表字段，列表统计） */
    @TableField(exist = false)
    private Long gameCount;
}
