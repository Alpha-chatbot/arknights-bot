package com.arknights.bot.infra.util;

import com.arknights.bot.domain.entity.ClassificationEnum;

import java.util.HashMap;
import java.util.Map;
import static com.arknights.bot.domain.entity.ClassificationEnum.*;

/**
 * 分类工具类
 *
 * Created by wangzhen on 2023/1/26 11:38
 * @author 14869
 */
public class ClassificationUtil {

    public static ClassificationEnum GetClass(String s) {
        Map<String, ClassificationEnum> map = new HashMap<>();

        /**
         * 新增内容:token录入或更新
         */
        map.put("token录入", TokenInsert);
        /**
         * 新增内容，寻访记录
         */
        map.put("寻访查询", GaCha);
        /**
         * 新增内容:获取粥的官网查询token教程
         */
        map.put("token获取教程", TokenHelp);

        map.put("菜单", CaiDan);
        map.put("功能", CaiDan);
        map.put("技能查询", SkillQuery);
        map.put("发红包", RED_ENVELOPE);

        return map.getOrDefault(s, TALKING);
    }
}
