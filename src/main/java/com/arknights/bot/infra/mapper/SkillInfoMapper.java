package com.arknights.bot.infra.mapper;

import com.arknights.bot.domain.entity.SkillLevelInfo;

import java.util.List;

/**
 * Created by wangzhen on 2023/2/5 22:58
 * @author 14869
 */
public interface SkillInfoMapper {
    /**
     * 查询技能信息
     * @param skillCode
     * @return
     */
    List<SkillLevelInfo> selectSkillInfoByCode(String skillCode);
}
