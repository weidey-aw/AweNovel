package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 资源（下载内容） gal_resource
 *
 * <p>资源本身只保存标题、类型、版本、大小等元信息；
 * 具体的网盘链接 / 提取码 / 解压码在 {@link ResourceSource}（gal_resource_source）中维护，
 * 一个资源可对应多条来源。</p>
 *
 * @author weidey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gal_resource")
public class Resource extends BaseGalEntity {

    /** 资源ID */
    @TableId(value = "resource_id", type = IdType.AUTO)
    private Long resourceId;

    /** 游戏ID */
    private Long gameId;

    /** 发布者ID（sys_user.user_id） */
    private Long userId;

    /** 资源标题 */
    private String title;

    /** 资源类型（字典 game_resource_type：netdisk/magnet/direct/torrent/other） */
    private String type;

    /**
     * 旧版单链接字段（保留兼容，已迁移到 gal_resource_source）
     *
     * @deprecated 使用 {@link #sources}
     */
    @Deprecated
    private String url;

    /**
     * 旧版解压密码字段（保留兼容）
     *
     * @deprecated 使用 {@link #sources}
     */
    @Deprecated
    private String extractPwd;

    /** 版本 */
    private String version;

    /** 大小 */
    private String size;

    /** 校验码 */
    private String checksum;

    /** 下载所需积分 */
    private Integer points;

    /** 下载次数 */
    private Integer downloadCount;

    /** 失效举报数 */
    private Integer reportCount;

    /** 审核状态（0待审核 1通过 2拒绝） */
    private String status;

    /** Flowable 流程实例ID */
    private String processInstanceId;

    /* ---------------- 非表字段 ---------------- */

    /** 资源来源列表（提交与详情返回） */
    @TableField(exist = false)
    private List<ResourceSource> sources;

    /** 来源数量 */
    @TableField(exist = false)
    private Long sourceCount;

    /** 发布者昵称 */
    @TableField(exist = false)
    private String userName;

    /** 发布者头像 */
    @TableField(exist = false)
    private String userAvatar;

    /** 游戏名称（管理端列表展示） */
    @TableField(exist = false)
    private String gameTitle;
}
