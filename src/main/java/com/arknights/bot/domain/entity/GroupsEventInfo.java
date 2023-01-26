package com.arknights.bot.domain.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 群聊事件实体
 * Created by wangzhen on 2023/1/19 20:17
 * @author 14869
 */
@Data
public class GroupsEventInfo implements Serializable {
    @ApiModelProperty(value = "消息类型, 常用的:TextMsg,AtMsg,PicMsg")
    private String msgType;
    @ApiModelProperty(value = "群聊id")
    private Long groupId;
    @ApiModelProperty(value = "GroupUserQQ")
    private Long qq;
    @ApiModelProperty(value = "消息对应账号群昵称")
    private String nickName;
    @ApiModelProperty(value = "事件data")
    private String eventData;
    @ApiModelProperty(value = "消息内容")
    private String content;
}
