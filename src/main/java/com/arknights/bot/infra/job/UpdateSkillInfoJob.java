package com.arknights.bot.infra.job;

import com.arknights.bot.app.service.ImportGameDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by wangzhen on 2023/2/2 10:08
 * @author 14869
 */
@Component
@Slf4j
public class UpdateSkillInfoJob {
    @Autowired
    private ImportGameDataService importGameDataService;

    @Scheduled(cron = "${scheduled.updateSkillInfoJob}")
    @Async
    public void updateSkillInfoJob() {
        try {
            importGameDataService.gameDataImport("技能导入");
        }catch (RuntimeException e){
            log.error("更新干员基本信息失败");
            e.printStackTrace();
        }
        log.info("干员基本信息调度完成");
    }
}
