package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 资源反馈 gal_resource_feedback
 *
 * @author weidey
 */
@Data
@TableName("gal_resource_feedback")
public class ResourceFeedback {

    /** 反馈ID */
    @TableId(value = "feedback_id", type = IdType.AUTO)
    private Long feedbackId;

    /** 资源ID */
    private Long resourceId;

    /** 反馈人 */
    private Long userId;

    /** 反馈类型（1资源失效 2无法运行 3缺少补丁 4其他） */
    private Integer type;

    /** 反馈内容 */
    private String content;

    /** 状态（0未读 1已读） */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /* ---------------- 非表字段 ---------------- */

    /** 反馈人昵称 */
    @TableField(exist = false)
    private String userName;

    /** 反馈人头像 */
    @TableField(exist = false)
    private String userAvatar;

    /** 资源标题 */
    @TableField(exist = false)
    private String resourceTitle;

    /** 所属游戏ID */
    @TableField(exist = false)
    private Long gameId;

    /** 所属游戏名 */
    @TableField(exist = false)
    private String gameTitle;

    /** 类型文案 */
    @TableField(exist = false)
    private String typeLabel;

    /** 反馈类型文案 */
    public static String typeLabel(Integer type) {
        if (type == null) {
            return "其他反馈";
        }
        return switch (type) {
            case 1 -> "资源失效";
            case 2 -> "无法运行";
            case 3 -> "缺少补丁";
            default -> "其他反馈";
        };
    }
}
