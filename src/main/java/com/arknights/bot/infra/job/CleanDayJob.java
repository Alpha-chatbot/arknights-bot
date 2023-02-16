package com.arknights.bot.infra.job;


import com.arknights.bot.infra.mapper.UserFoundMapper;
import com.arknights.bot.infra.util.SendMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 剿灭定时提醒
 *
 * Created by wangzhen on 2023/1/20 11:16
 * @author 14869
 */

@Component
@Slf4j
public class CleanDayJob {

    @Resource
    private UserFoundMapper userFoundMapper;
    @Autowired
    private SendMsgUtil sendMsgUtil;

    /**
     * 每周日下午五点提示剿灭结算
     */
    @Scheduled(cron = "${scheduled.gameCleanJob}")
    @Async
    public void cleanDayJob() {
        List<Long> groups = userFoundMapper.selectAllGroups();
        for (Long groupId : groups) {
            sendMsgUtil.CallOPQApiSendMsg(groupId, "我是本群剿灭小助手，今天是本周最后一天，博士不要忘记打剿灭哦❤\n", 2);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                log.info(ignored.getMessage());
            }
        }
    }
}
