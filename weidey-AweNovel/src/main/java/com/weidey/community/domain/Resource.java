package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资源（下载内容） gal_resource
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

    /** 发布者ID */
    private Long userId;

    /** 资源标题 */
    private String title;

    /** 类型（netdisk网盘/magnet磁力/torrent种子） */
    private String type;

    /** 链接 */
    private String url;

    /** 版本 */
    private String version;

    /** 大小 */
    private String size;

    /** 解压密码 */
    private String extractPwd;

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
}
