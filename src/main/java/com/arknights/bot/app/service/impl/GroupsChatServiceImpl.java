package com.arknights.bot.app.service.impl;


import com.arknights.bot.app.service.GroupsChatService;
import com.arknights.bot.domain.entity.GroupsEventInfo;
import com.arknights.bot.infra.util.SendMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;


/**
 * Created by wangzhen on 2023/1/19 20:32
 *
 * @author 14869
 */
@Slf4j
@Service
public class GroupsChatServiceImpl implements GroupsChatService {

    @Value("${userConfig.loginQq}")
    private Long loginAccount;
    @Autowired
    private GroupsChatService groupsChatService;

    @Autowired
    private SendMsgUtil sendMsgUtil;


    @Override
    public String generalMessageHandler(GroupsEventInfo groupsEventInfo) {
        return null;
    }

    @Override
    public void eventMessageHandler(GroupsEventInfo message) {
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
                groupsChatService.sendMessage(groupId, eventData.getLong("UserID"),
                        "欢迎" + eventData.getString("UserName") + "入群");
                break;
            case "ON_EVENT_GROUP_REVOKE":
                //撤回消息事件
                result = "";
                eventData = new JSONObject(message.getEventData());
                groupsChatService.sendMessage(groupId, eventData.getLong("UserID"),
                        "谁刚刚撤回了消息,让我看看!!");
                break;
            default:
                result = "";
        }
    }

    @Override
    public void sendMessage(Long groupId, Long qq, String text) {
        sendMsgUtil.CallOPQApiSendMsg(groupId, text, 2);
    }

}
