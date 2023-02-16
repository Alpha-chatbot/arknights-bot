package com.arknights.bot.infra.job;

import com.arknights.bot.infra.mapper.GaChaInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by wangzhen on 2023/2/16 14:35
 * @author 14869
 */

@Component
@Slf4j
public class CleanDataJob {

    @Resource
    private GaChaInfoMapper gaChaInfoMapper;

    /**
     * 每周一凌晨1点清理数据
     */
    @Scheduled(cron = "${scheduled.cleanJob}")
    @Async
    public void cleanDayJob() {
        // 定时清理寻访数据
        gaChaInfoMapper.cleanGaChaInfo();
    }
}
