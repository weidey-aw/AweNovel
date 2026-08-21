package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 游戏条目 gal_game
 *
 * @author weidey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gal_game")
public class Game extends BaseGalEntity {

    /** 游戏ID */
    @TableId(value = "game_id", type = IdType.AUTO)
    private Long gameId;

    /** 制作会社ID */
    private Long brandId;

    /** 游戏原名 */
    private String title;

    /** 游戏译名 */
    private String titleCn;

    /** 封面图 */
    private String cover;

    /** 发售日期 */
    private Date releaseDate;

    /** 简介 */
    private String summary;

    /** 原画 */
    private String staffPaint;

    /** 剧本 */
    private String staffScenario;

    /** 主要声优 */
    private String staffVoice;

    /** 平均评分（冗余） */
    private BigDecimal ratingAvg;

    /** 评分人数（冗余） */
    private Integer ratingCount;

    /** 浏览量 */
    private Integer viewCount;

    /** 状态（1上架 0下架） */
    private String status;

    /** 标签列表（非表字段） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<Tag> tags;

    /** 标签ID列表（非表字段，提交用） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<Long> tagIds;

    /** 会社名称（非表字段，连表展示用） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String brandName;
}
