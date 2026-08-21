package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章（资讯/评测/攻略） gal_article
 *
 * @author weidey
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gal_article")
public class Article extends BaseGalEntity {

    /** 文章ID */
    @TableId(value = "article_id", type = IdType.AUTO)
    private Long articleId;

    /** 作者ID */
    private Long userId;

    /** 标题 */
    private String title;

    /** 简介 */
    private String summary;

    /** 内容 */
    private String content;

    /** 封面图 */
    private String cover;

    /** 分类（news资讯/review评测/guide攻略） */
    private String category;

    /** 审核状态（0待审核 1通过 2拒绝） */
    private String status;

    /** Flowable 流程实例ID */
    private String processInstanceId;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;
}
