package com.arknights.bot.infra.mapper;

import com.arknights.bot.domain.entity.AccountInfo;
import com.arknights.bot.domain.entity.OperatorBaseInfo;
import com.arknights.bot.domain.entity.SkillLevelInfo;

import java.util.List;

/**
 * Created by wangzhen on 2023/2/1 19:55
 * @author 14869
 */
public interface ImportGameDataMapper {

    /**
     * 查询干员列表
      * @param name
     * @return
     */
    List<OperatorBaseInfo> selectOperatorInfo(String name);
    /**
     * 插入干员基本信息
     * @param operatorBaseInfo
     * @return
     */
    Integer insertOperatorBaseInfo(OperatorBaseInfo operatorBaseInfo);

    /**
     * 插入技能信息
     * @param skillLevelInfo
     * @return
     */
    Integer insertSkillInfo(SkillLevelInfo skillLevelInfo);

    /**
     * 查询技能信息
     * @param skillCode
     * @return
     */
    List<SkillLevelInfo> selectSkillInfoByCode(String skillCode);

    /**
     * 更新技能信息
     * @param skillLevelInfo
     * @return
     */
    int updateSkillInfoById(SkillLevelInfo skillLevelInfo);

    /**
     * 清理技能数据
     */
    void cleanSkillInfo();

    /**
     * 清理干员数据方便更新
     */
    void cleanOperatorInfo();

    int updateErrorInfoById(SkillLevelInfo skillLevelInfo);
}
