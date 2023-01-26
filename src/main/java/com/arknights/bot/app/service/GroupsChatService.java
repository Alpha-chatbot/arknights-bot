package com.arknights.bot.app.service;

import com.arknights.bot.domain.entity.GroupsEventInfo;


/**
 * Created by wangzhen on 2023/1/19 20:31
 * @author 14869
 */
public interface GroupsChatService {

    /**
     * 群聊模式消息获取并转发处理
     * @param groupsEventInfo
     */
    String groupMessageHandler (GroupsEventInfo groupsEventInfo);

    /**
     * 入群事件和撤回事件 相关回复
     * @param groupId
     * @param qq
     * @param text
     */
    void autoEvent(Long groupId, Long qq, String text);
}
