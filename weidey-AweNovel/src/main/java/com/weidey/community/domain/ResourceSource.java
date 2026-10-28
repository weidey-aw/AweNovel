package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 资源来源 gal_resource_source
 *
 * <p>一个资源可有多条来源（不同网盘 / 磁力 / 直链），每条来源有自己的提取码、解压码。</p>
 *
 * @author weidey
 */
@Data
@TableName("gal_resource_source")
public class ResourceSource {

    /** 来源ID */
    @TableId(value = "source_id", type = IdType.AUTO)
    private Long sourceId;

    /** 资源ID */
    private Long resourceId;

    /** 来源类型（netdisk/magnet/direct/torrent/other） */
    private String sourceType;

    /** 资源链接 */
    private String url;

    /** 提取码 */
    private String extractPwd;

    /** 解压码 */
    private String unzipPwd;

    /** 备注 */
    private String remark;

    /** 排序 */
    private Integer sort;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
