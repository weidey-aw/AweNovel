package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 游戏正版资源 gal_official_resource
 *
 * <p>与用户无关，一个游戏可配置多条正版来源（官网 / 商店 / Steam / DLsite …），
 * 前台详情页优先展示。</p>
 *
 * @author weidey
 */
@Data
@TableName("gal_official_resource")
public class OfficialResource {

    /** 正版资源ID */
    @TableId(value = "official_id", type = IdType.AUTO)
    private Long officialId;

    /** 游戏ID */
    private Long gameId;

    /** 标题 */
    private String title;

    /** 类型（official/store/steam/dlsite/other） */
    private String sourceType;

    /** 官方链接 */
    private String url;

    /** 小图标（相对路径或图标名） */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
