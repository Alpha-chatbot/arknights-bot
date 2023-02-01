package com.arknights.bot.app.service;

import com.arknights.bot.domain.entity.GroupsEventInfo;

/**
 * Created by wangzhen on 2023/2/1 18:01
 * @author 14869
 */
public interface ImportGameDataService {

    /**
     * 干员基础信息导入
     * @param content
     */
    void OperatorBaseInfoImport(String content);
}
