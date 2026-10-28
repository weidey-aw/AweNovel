package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 制作会社（品牌） gal_brand
 *
 * @author weidey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gal_brand")
public class Brand extends BaseGalEntity {

    /** 会社ID */
    @TableId(value = "brand_id", type = IdType.AUTO)
    private Long brandId;

    /** 会社名称 */
    private String name;

    /** Logo */
    private String logo;

    /** 官方网站（外链，可空） */
    private String website;

    /** 会社简介 */
    private String description;

    /** 关联游戏数（非表字段，列表统计） */
    @TableField(exist = false)
    private Long gameCount;
}
