package com.weidey.community.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 社区实体基类（逻辑删除 + 时间自动填充）
 *
 * @author weidey
 */
@Data
public class BaseGalEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 删除标志（0存在 2删除） */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
