package com.arknights.bot.infra.job;

import com.arknights.bot.app.service.ImportGameDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时更新干员列表
 * Created by wangzhen on 2023/2/1 20:49
 *
 * @author 14869
 */
@Component
@Slf4j
public class UpdateOperatorBaseDataJob {

    @Autowired
    private ImportGameDataService importGameDataService;

    /**
     * 干员等游戏数据取自json解析，json文件来源于https://github.com/Kengxxiao/ArknightsGameData ，后续可能添加github库远程自动下载方法,
     * 现在是手动下载xxx.json上传到服务器指定路径下
     */
    @Scheduled(cron = "${scheduled.updateOperatorBaseJob}")
    @Async
    public void updateOperatorBaseJob() {
        try {
            importGameDataService.gameDataImport("干员导入");
        }catch (RuntimeException e){
            log.error("更新干员基本信息失败");
            e.printStackTrace();
        }
        log.info("干员基本信息调度完成");
    }
}
