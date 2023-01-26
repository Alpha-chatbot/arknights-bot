package com.arknights.bot.app.service;

import com.arknights.bot.domain.entity.GroupsEventInfo;


/**
 * Created by wangzhen on 2023/1/19 20:31
 * @author 14869
 */
public interface GroupsChatService {

    /**
     * 群聊模式普通消息获取并处理
     * @param groupsEventInfo
     */
    String generalMessageHandler (GroupsEventInfo groupsEventInfo);

    /**
     * 入群事件和撤回事件 相关回复
     * @param groupsEventInfo
     */
    void eventMessageHandler(GroupsEventInfo groupsEventInfo);

    /**
     * 判断并回复消息
     * @param groupId
     * @param qq
     * @param text
     */
    void sendMessage(Long groupId, Long qq, String text);

    /**
     * 更新token信息
     * @param token
     * @param qq
     * @return
     */
    public String insertOrUpdateToken(String token, Long qq);
}
