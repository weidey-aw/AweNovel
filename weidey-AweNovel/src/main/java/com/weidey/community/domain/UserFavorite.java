package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 用户收藏 gal_user_favorite
 *
 * @author weidey
 */
@Data
@TableName("gal_user_favorite")
public class UserFavorite {

    /** ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 目标类型（G游戏 A文章 R资源） */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /* ---------------- 非表字段（列表展示） ---------------- */

    /** 标题 */
    @TableField(exist = false)
    private String title;

    /** 封面 */
    @TableField(exist = false)
    private String cover;

    /** 副标题（分类 / 会社等） */
    @TableField(exist = false)
    private String subtitle;

    /** 前端跳转地址 */
    @TableField(exist = false)
    private String url;

    /** 类型文案 */
    @TableField(exist = false)
    private String typeLabel;
}
