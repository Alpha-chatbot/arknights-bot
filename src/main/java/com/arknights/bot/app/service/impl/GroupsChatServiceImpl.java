package com.arknights.bot.app.service.impl;


import com.arknights.bot.app.service.GroupsChatService;
import com.arknights.bot.domain.entity.GroupsEventInfo;
import lombok.extern.slf4j.Slf4j;
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


    @Override
    public String groupMessageHandler(GroupsEventInfo groupsEventInfo) {
        return null;
    }

    @Override
    public void autoEvent(Long groupId, Long qq, String text) {

    }
}
