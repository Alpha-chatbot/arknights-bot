package com.arknights.bot.api.controller;

import com.arknights.bot.app.service.GroupsChatService;
import com.arknights.bot.domain.entity.GroupsEventInfo;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bot群聊调用功能相关
 *
 * Created by wangzhen on 2023/1/19 20:02
 * @author 14869
 */
@Slf4j
@RequestMapping("/groups")
@RestController
public class GroupsChatController {

    @Autowired
    private GroupsChatService groupsChatService;

    /**
     * 群聊消息获取并分类处理
     * @param groupsEventInfo
     * @return
     */
    @PostMapping("/receive")
    public String receiveMessage(@RequestBody GroupsEventInfo groupsEventInfo) {
        return groupsChatService.groupMessageHandler(groupsEventInfo);
    }

    @PostMapping("/autoEvent")
    public String receive(
            @RequestBody GroupsEventInfo message
    ) {
        Long qq = message.getQq();
        Long groupId = message.getGroupId();
        log.info("接受到事件消息:{}", message.getContent());
        String type = message.getMsgType();
        String result;
        JSONObject eventData;
        switch (type) {
            case "ON_EVENT_GROUP_JOIN":
                //入群事件
                result = "";
                eventData = new JSONObject(message.getEventData());
                groupsChatService.autoEvent(groupId, eventData.getLong("UserID"),
                        "欢迎" + eventData.getString("UserName") + "入群");
                break;
            case "ON_EVENT_GROUP_REVOKE":
                //撤回消息事件
                result = "";
                eventData = new JSONObject(message.getEventData());
                groupsChatService.autoEvent(groupId, eventData.getLong("UserID"),
                        "谁刚刚撤回了消息,让我看看!!");
                break;
            default:
                result = "";
        }
        return result;
    }

}
