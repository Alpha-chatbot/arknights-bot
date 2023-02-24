package com.arknights.bot.infra.util;

import com.arknights.bot.domain.entity.ClassificationEnum;

import java.util.HashMap;
import java.util.Map;

import static com.arknights.bot.domain.entity.ClassificationEnum.*;
import static com.arknights.bot.domain.entity.ClassificationEnum.TALKING;

/**
 * 特殊消息，不以#开头
 * Created by wangzhen on 2023/2/24 18:07
 */
public class SpecialConstanceUtil {

    public static ClassificationEnum GetClass(String s) {
        Map<String, ClassificationEnum> map = new HashMap<>();
        map.put("撅你", FK);

        return map.getOrDefault(s, null);
    }
}
