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
}
