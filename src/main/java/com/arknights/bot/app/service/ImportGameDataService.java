package com.arknights.bot.app.service;

import com.arknights.bot.domain.entity.GroupsEventInfo;
import com.arknights.bot.domain.entity.OperatorBaseInfo;
import com.arknights.bot.domain.entity.SkillLevelInfo;
import com.arknights.bot.domain.entity.SkillMappingInfo;

import java.util.List;

/**
 * Created by wangzhen on 2023/2/1 18:01
 * @author 14869
 */
public interface ImportGameDataService {

    /**
     * 导入操作
     * @param content
     */
    void gameDataImport(String content);

    /**
     * 干员信息导入
     * @param content
     */
    void operatorBaseInfoImport(String content);

    /**
     * 技能信息导入
     * @param content
     */
    void skillInfoImport(String content);

    /**
     * 插入干员数据
     * @param operatorBaseInfoList
     */
    void insertOperatorInfo(List<OperatorBaseInfo> operatorBaseInfoList, List<SkillMappingInfo> skillLevelInfoList);

    /**
     * 处理技能字符串
     * @param result
     * @param order
     */
    void handleSkillInfo(String result, Integer order);

    /**
     * 插入技能数据
     * @param skillLevelInfoList
     */
    void insertSkillInfo(List<SkillLevelInfo> skillLevelInfoList);

    /**
     * 更新技能数据
     * @param skillLevelInfoList
     */
    void updateSkillInfo(List<SkillLevelInfo> skillLevelInfoList);
}
