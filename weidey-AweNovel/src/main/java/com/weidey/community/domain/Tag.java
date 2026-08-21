package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private Date createTime;
}
