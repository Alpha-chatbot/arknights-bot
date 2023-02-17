package com.arknights.bot.api.controller;

import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.system.SystemUtil;
import com.arknights.bot.app.service.GroupsChatService;
import com.arknights.bot.domain.entity.GroupsEventInfo;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Bot群聊调用功能相关
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

    /**
     * 测试获取客户端（浏览器信息）
     * @param request
     */
    @GetMapping("/print")
    public void toPrintAgentInfo(HttpServletRequest request){
        log.info("--------------------------服务端信息-----------------------------");
        SystemUtil.dumpSystemInfo();
        log.info("--------------------------客户端信息-----------------------------");
        UserAgent ua = UserAgentUtil.parse(request.getHeader(Header.USER_AGENT.toString()));
        log.info(ua.getPlatform().toString());
        log.info(JSONUtil.toJsonStr(ua));
        log.info("-------------------------Over----------------------------------");
    }


}
