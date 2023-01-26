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
 * <p>
 * Created by wangzhen on 2023/1/19 20:02
 *
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
     *
     * @param groupsEventInfo
     * @return
     */
    @PostMapping("/general-message")
    public String receiveGeneralMessage(@RequestBody GroupsEventInfo groupsEventInfo) {
        return groupsChatService.generalMessageHandler(groupsEventInfo);
    }

    /**
     * 事件消息获取与处理
     * @param message
     */
    @PostMapping("/event-message")
    public void receiveAutoEventMessage(
            @RequestBody GroupsEventInfo message
    ) {
        groupsChatService.eventMessageHandler(message);
    }

}
