package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 评分 gal_rating
 *
 * @author weidey
 */
@Data
@TableName("gal_rating")
public class Rating {

    /** 评分ID */
    @TableId(value = "rating_id", type = IdType.AUTO)
    private Long ratingId;

    /** 用户ID */
    private Long userId;

    /** 游戏ID */
    private Long gameId;

    /** 评分（1-10） */
    private Integer score;

    /** 创建时间 */
    private Date createTime;
}
