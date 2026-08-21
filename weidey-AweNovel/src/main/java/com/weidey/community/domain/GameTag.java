package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游戏-标签关联 gal_game_tag
 *
 * @author weidey
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("gal_game_tag")
public class GameTag {

    /** 游戏ID */
    private Long gameId;

    /** 标签ID */
    private Long tagId;
}
