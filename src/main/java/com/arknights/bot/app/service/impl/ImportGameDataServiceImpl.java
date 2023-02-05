package com.arknights.bot.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arknights.bot.api.dto.SpDataDto;
import com.arknights.bot.app.service.ImportGameDataService;
import com.arknights.bot.domain.entity.OperatorBaseInfo;
import com.arknights.bot.domain.entity.PageRequest;
import com.arknights.bot.domain.entity.SkillLevelInfo;
import com.arknights.bot.infra.constant.Constance;
import com.arknights.bot.infra.mapper.ImportGameDataMapper;
import com.arknights.bot.infra.util.RequestMsgUtil;
import com.sun.org.apache.xalan.internal.xsltc.runtime.ErrorMessages_zh_CN;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.arknights.bot.infra.util.TextToImageUtil.replaceEnter;

/**
 * Created by wangzhen on 2023/2/1 18:00
 *
 * @author 14869
 */
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@Slf4j
@Service
public class ImportGameDataServiceImpl implements ImportGameDataService {

    private static final String BILI_WIKI_URL = "https://wiki.biligame.com/arknights/";
    private static final String PRTS_WIKI_URL = "https://prts.wiki/index.php";


    @Autowired
    private RequestMsgUtil requestMsgUtil;
    @Resource
    private ImportGameDataMapper importGameDataMapper;

    @Override
    public void gameDataImport(String content) {
        if (StringUtils.isBlank(content)) {
            log.info("导入内容为空");
            return;
        }
        switch (content) {
            case "干员导入":
                // 导入干员列表
                operatorBaseInfoImport(content);
                break;
            case "技能导入":
                // 导入干员技能信息
                skillInfoImport(content);
                break;
            default:
                log.info("导入内容无分类");
        }
    }

    @Override
    public void operatorBaseInfoImport(String content) {

        importGameDataMapper.cleanOperatorInfo();
        importGameDataMapper.cleanSkillInfo();
        String uploadFileSavePath = "F:\\backup";
        // String uploadFileSavePath = FILE_PATH_PREFIX;

        // 下载文件进行解析
        // File downloadFile = new File(uploadFileSavePath + File.separator + "character_table.json");
        File jsonFile = new File(uploadFileSavePath + File.separator + "character_table.json");
        //通过上面那个方法获取json文件的内容
        String jsonData = getStr(jsonFile);
        //转json对象
        com.alibaba.fastjson.JSONObject parse = (com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSONObject.parse(jsonData);
        //获取主要数据
        //遍历key和value
        List<OperatorBaseInfo> operatorBaseInfoList = new ArrayList<>();
        List<SkillLevelInfo> skillLevelInfoList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : parse.entrySet()) {
            String key = entry.getKey();
            com.alibaba.fastjson.JSONObject valueObject = (com.alibaba.fastjson.JSONObject) entry.getValue();
            // 干员中文名称
            String zhName = valueObject.getString("name");
            // 干员英文名
            String enName = valueObject.getString("appellation");
            // 转为小写方便匹配其余json信息
            enName = enName.toLowerCase();
            // 招聘合同
            String itemUsage = valueObject.getString("itemUsage");
            // 潜能物id，这里用于排除道具类的数据,吐槽:为什么道具和召唤物也会放在人物表里...这里暂时先只统计干员
            String potentialItemId = valueObject.getString("potentialItemId");
            if (StringUtils.isBlank(potentialItemId)) {
                continue;
            }
            OperatorBaseInfo operatorBaseInfo = new OperatorBaseInfo();
            operatorBaseInfo.setKeyCode(key);
            operatorBaseInfo.setEnName(enName);
            operatorBaseInfo.setZhName(zhName);
            operatorBaseInfo.setItemUsage(itemUsage);

            // 获取技能id,先插表后通过skillId再去匹配skill_table
            JSONArray skillArr = valueObject.getJSONArray("skills");
            int order = 1;
            String mappingCode = "";
            for (int j = 0; j < skillArr.size(); j++) {
                com.alibaba.fastjson.JSONObject jsonObject = skillArr.getJSONObject(j);
                String skillId = jsonObject.getString("skillId");
                if(j==0){
                    // 截取部分可作为技能表和干员表的关联字段(待测试)
                    String[] perm = skillId.split("_");
                    if(perm.length>=2){
                        mappingCode = perm[0] + "_" + perm[1];
                        operatorBaseInfo.setMappingCode(mappingCode);
                    }
                }
                // 解锁条件
                JSONObject unlockCond = jsonObject.getJSONObject("unlockCond");
                // 0表示精英0开放
                String openLevel = unlockCond.getString("phase");
                log.info("技能信息: 技能id:{}, 技能解锁条件:{}, 技能序号:{}", skillId, openLevel, order);
                SkillLevelInfo skillLevelInfo = new SkillLevelInfo();
                skillLevelInfo.setSkillCode(skillId);
                skillLevelInfo.setOpenLevel(openLevel);
                skillLevelInfo.setSkillOrder(order);
                skillLevelInfoList.add(skillLevelInfo);
                order++;
            }
            log.info("干员基本信息: key值:{},干员中文名:{}, 干员英文名:{}, 招聘合同:{}, 映射code:{}", key, zhName, enName, itemUsage, mappingCode);

            operatorBaseInfoList.add(operatorBaseInfo);
        }

        log.info("统计干员{}名,技能list大小:{}，准备插表", operatorBaseInfoList.size(), skillLevelInfoList.size());
        if (!CollectionUtils.isEmpty(operatorBaseInfoList) && !CollectionUtils.isEmpty(skillLevelInfoList)) {
            ImportGameDataService inf = (ImportGameDataService) AopContext.currentProxy();
            inf.insertOperatorInfo(operatorBaseInfoList, skillLevelInfoList);
            log.info("数据插入完成~");
        } else {
            log.error("数据为空, 需要检查数据");
        }
    }

    /**
     * 干员技能信息导入
     *
     * @param content
     */
    @Override
    public void skillInfoImport(String content) {

        String uploadFileSavePath = "F:\\backup";
        // String uploadFileSavePath = FILE_PATH_PREFIX;

        // 下载文件进行解析
        // File downloadFile = new File(uploadFileSavePath + File.separator + "character_table.json");
        File jsonFile = new File(uploadFileSavePath + File.separator + "skill_table.json");
        //通过上面那个方法获取json文件的内容
        String jsonData = getStr(jsonFile);
        //转json对象
        com.alibaba.fastjson.JSONObject parse = (com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSONObject.parse(jsonData);
        //获取主要数据
        //遍历key和value
        List<SkillLevelInfo> skillUpdateList = new ArrayList<>();
        List<SkillLevelInfo> skillInsertList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : parse.entrySet()) {
            // key对应skillCode
            String key = entry.getKey();
            com.alibaba.fastjson.JSONObject valueObject = (com.alibaba.fastjson.JSONObject) entry.getValue();
            // skillId对应值也应与一级key相同
            String skillCode = valueObject.getString("skillId");
            // 查询先前插入的skillId和序号 保留openLevel和skillOrder
            List<SkillLevelInfo> skillLevelInfoList = importGameDataMapper.selectSkillInfoByCode(skillCode);
            if (CollectionUtils.isEmpty(skillLevelInfoList) || skillLevelInfoList.size() > 1) {
                continue;
            }
            SkillLevelInfo skillLevelInfo = skillLevelInfoList.get(0);
            String openLevel = skillLevelInfo.getOpenLevel();
            Integer skillOrder = skillLevelInfo.getSkillOrder();

            // 获取技能详细信息，levels长度为10，对应10个技能等级（1-7，专1-3）
            JSONArray skillArr = valueObject.getJSONArray("levels");
            String skillId = valueObject.getString("skillId");
            for (int j = 1; j <= skillArr.size(); j++) {
                com.alibaba.fastjson.JSONObject jsonObject = skillArr.getJSONObject(j-1);
                // 技能名（中）
                String skillName = jsonObject.getString("name");
                // 技能描述
                String description = jsonObject.getString("description");
                // 1表示手动触发 2表示自动触发
                String triggerType = jsonObject.getString("skillType");
                if ("1".equals(triggerType)) {
                    triggerType = Constance.TYPE_F;
                } else if ("2".equals(triggerType)) {
                    triggerType = Constance.TYPE_S;
                }
                // 技力相关
                JSONObject spData = jsonObject.getJSONObject("spData");
                // json转为实体类
                SpDataDto spDataDto = JSON.toJavaObject(spData, SpDataDto.class);
                // 技力类型(1为自动回复,2为攻击回复,8好像为召唤物类型)
                String powerType = spDataDto.getSpType();
                if ("1".equals(powerType)) {
                    powerType = Constance.TYPE_SP_F;
                } else if ("2".equals(powerType)) {
                    powerType = Constance.TYPE_SP_S;
                }
                // 初始技力
                Long initialValue = Long.valueOf(spDataDto.getInitSp());
                // 技力消耗
                Long consumeValue = Long.valueOf(spDataDto.getSpCost());
                if (ObjectUtils.isEmpty(spDataDto)) {
                    log.info("技力相关解析数据异常");
                    continue;
                }
                // 技能持续时间
                String duration = jsonObject.getString("duration");
                if (Float.parseFloat(duration) < 0) {
                    duration = "0";
                }
                // 技能描述中的变量k-v
                JSONArray blackboard = jsonObject.getJSONArray("blackboard");
                Map<String, String> descMap = new HashMap<>();
                for (int k = 1; k <= blackboard.size(); k++) {
                    com.alibaba.fastjson.JSONObject kvObject = blackboard.getJSONObject(k-1);
                    descMap.put(kvObject.getString("key"), kvObject.getString("value"));
                }
                log.info("打印map:{}", descMap);
                try {
                    // 处理多余字符
                    description = handleDesc(description, descMap);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                log.info("技能信息: 技能id:{}, 技能描述:{}", skillId, description);

                SkillLevelInfo skillInfo = SkillLevelInfo.builder().skillCode(skillCode).skillName(skillName).description(description).triggerType(triggerType)
                        .powerType(powerType).initialValue(initialValue).consumeValue(consumeValue).span((long)Float.parseFloat(duration))
                        .skillLevel((long) j).skillOrder(skillOrder).openLevel(openLevel).build();
                // 对原数据进行更新，以及插入新数据
                if (j == 1) {
                    skillInfo.setId(skillLevelInfo.getId());
                    skillUpdateList.add(skillInfo);
                    log.info("此行准备更新,更新内容:{}", skillInfo);
                } else {
                    skillInsertList.add(skillInfo);
                }
            }
            // 非事务调用事务方法需要代理调用
            ImportGameDataService inf = (ImportGameDataService) AopContext.currentProxy();
            // 更新
            if (!CollectionUtils.isEmpty(skillUpdateList)) {
                    inf.updateSkillInfo(skillUpdateList);
            } else {
                log.error("数据为空, 需要检查数据");
            }
            // 插入
            if (!CollectionUtils.isEmpty(skillInsertList)) {
                inf.insertSkillInfo(skillInsertList);
            } else {
                log.error("数据为空, 需要检查数据");
            }
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insertOperatorInfo(List<OperatorBaseInfo> operatorBaseInfoList, List<SkillLevelInfo> skillLevelInfoList) {
        // 干员基础信息
        for (OperatorBaseInfo line : operatorBaseInfoList) {
            importGameDataMapper.insertOperatorBaseInfo(line);
        }
        // 技能部分信息
        for (SkillLevelInfo skillLevelInfo : skillLevelInfoList) {
            importGameDataMapper.insertSkillInfo(skillLevelInfo);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insertSkillInfo(List<SkillLevelInfo> skillLevelInfoList) {
        for (SkillLevelInfo skillLevelInfo : skillLevelInfoList) {
            importGameDataMapper.insertSkillInfo(skillLevelInfo);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateSkillInfo(List<SkillLevelInfo> skillLevelInfoList) {
        // 技能部分信息
        for (SkillLevelInfo skillLevelInfo : skillLevelInfoList) {
            try {
                importGameDataMapper.updateSkillInfoById(skillLevelInfo);
            } catch (Exception e){
                skillLevelInfo.setAttribute1(e.getMessage().substring(0, 60));
                importGameDataMapper.updateErrorInfoById(skillLevelInfo);
            }
        }
    }

    /**
     * 把一个文件中的内容读取为字符串
     *
     * @param jsonFile
     * @return
     */
    public static String getStr(File jsonFile) {
        String jsonStr = "";
        FileReader fileReader = null;
        Reader reader = null;
        try {
            fileReader = new FileReader(jsonFile);
            reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8);
            int ch = 0;
            StringBuilder sb = new StringBuilder();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String handleDesc(String str, Map<String, String> descMap) {
        // 处理字符串中的无用字符
        // 去除回车符
        str = replaceEnter(str);
        // 去除转义回车
        String regexEnter = "\\\\n";
        str = str.replaceAll(regexEnter, "");
        // 去除右斜杠
        String regexSlash = "\\\\";
        str = str.replaceAll(regexSlash, ",");
        // 移除术语标识
        Pattern pattern1 = Pattern.compile("([\\+\\-\\*]\\{)[\\+\\-\\*]");
        Matcher matcher1 = pattern1.matcher(str);
        int count = 0;
        while (matcher1.find()) {
            count++;
        }
        String result = matcher1.replaceAll("\\{");

        Pattern pattern2 = Pattern.compile("(<\\/\\>)");
        Matcher matcher2 = pattern2.matcher(result);
        count = 0;
        while (matcher2.find()) {
            count++;
        }
        // 统一替换
        result = matcher2.replaceAll("");

        String regex = "(\\{)|(\\}|(\\|)|(:))";
        result = result.replaceAll(regex, "");
        // 这里不能用replaceAll，因为用了的话不添加转义字符会无法识别替换，而且每个变量只出现一次，用replace即可
        for (Map.Entry<String, String> map : descMap.entrySet()) {
            String regexTemp = map.getKey();
            String value = map.getValue();
            result = result.replace(regexTemp, value);
            // 默认为小写,但是测试时发现有的description中 key是大写变量，所以还得考虑大小写
            regexTemp = regexTemp.toUpperCase();
            result = result.replace(regexTemp, value);

            float v = Float.parseFloat(value);
            v = v*100;
            // 在这里处理百分号转换问题，比如现在格式是 攻击力+0.40% ,替换为 40%
            String regexPercent = "[0-9][.][0-9]0%";
            result = result.replaceAll(regexPercent, String.valueOf((long)v)+"%");
        }
        String regexP = "(<\\$ba\\.[a-z]{1,8}>)|(<@ba\\.[a-z]{1,8}>)|(<\\$ba\\.[a-z]{1,9}\\.[a-z]{1,9}>)";
        result = result.replaceAll(regexP, "");
        log.info("最终替换后:{}", result);
        return result;



    }

    @Override
    public void handleSkillInfo(String result, Integer order) {
        log.info("按连续空格拆分行");
        String[] strArray = result.split("\\s+");
        if (ObjectUtils.isEmpty(strArray) || strArray.length < Constance.STR_LENGTH) {
            log.info("解析异常，技能资料不全");
            return;
        }
        String openLevel = strArray[0];
        String skillName = strArray[2];
        if (skillName.startsWith(Constance.SKILL_NAME_CH)) {
            skillName = skillName.substring(Constance.SKILL_NAME_CH.length());
        } else {
            log.info("解析异常，技能匹配错位,当前skillName为:{}", skillName);
            return;
        }
        List<SkillLevelInfo> skillLevelInfoList = new ArrayList<>();
        // 1 ~ 10对应技能1到7，专一到专三
        for (int i = 1; i <= Constance.SKILL_COUNTS; i++) {
            SkillLevelInfo skillLevelInfo = new SkillLevelInfo();
            StringBuilder remarks = new StringBuilder();
            for (String line : strArray) {
                // 技能序号，比如技能一为1
                skillLevelInfo.setSkillOrder(order);
                // 技能等级
                skillLevelInfo.setSkillLevel((long) i);
                // 技能解锁等级
                skillLevelInfo.setOpenLevel(openLevel);
                // 技能名
                skillLevelInfo.setSkillName(skillName);
                // 技力类型和触发类型
                if (line.startsWith(Constance.SKILL_TYPE_ONE)) {
                    skillLevelInfo.setPowerType(line.substring(Constance.SKILL_TYPE_ONE.length()));
                } else if (line.startsWith(Constance.SKILL_TYPE_TWO)) {
                    skillLevelInfo.setTriggerType(line.substring(Constance.SKILL_TYPE_TWO.length()));
                }

                // 每个技能等级对应的描述，初始值，消耗，持续时间
                String skillDesc = Constance.SKILL + i + Constance.SKILL_DESC;
                String skillInit = Constance.SKILL + i + Constance.SKILL_INIT;
                String skillConsume = Constance.SKILL + i + Constance.SKILL_CONSUME;
                String skillSpan = Constance.SKILL + i + Constance.SKILL_SPAN;
                int j = 0;
                if (i > 7) {
                    j = i - 7;
                }
                String skillSpecialiDesc = Constance.SKILL_SPECIALI + j + Constance.SKILL_DESC;
                String skillSpecialiInit = Constance.SKILL_SPECIALI + j + Constance.SKILL_INIT;
                String skillSpecialiConsume = Constance.SKILL_SPECIALI + j + Constance.SKILL_CONSUME;
                String skillSpecialiSpan = Constance.SKILL_SPECIALI + j + Constance.SKILL_SPAN;
                if (i <= 7) {
                    if (line.startsWith(skillDesc)) {
                        skillLevelInfo.setDescription(line.substring(skillDesc.length()));
                    } else if (line.startsWith(skillInit)) {
                        if (line.length() != skillInit.length()) {
                            skillLevelInfo.setInitialValue(Long.valueOf(line.substring(skillInit.length())));
                        } else {
                            skillLevelInfo.setInitialValue(0L);
                        }
                    } else if (line.startsWith(skillConsume)) {
                        if (line.length() != skillConsume.length()) {
                            skillLevelInfo.setConsumeValue(Long.valueOf(line.substring(skillConsume.length())));
                        } else {
                            skillLevelInfo.setConsumeValue(0L);
                        }
                    } else if (line.startsWith(skillSpan)) {
                        if (line.length() != skillSpan.length()) {
                            skillLevelInfo.setSpan(Long.valueOf(line.substring(skillSpan.length())));
                        } else {
                            skillLevelInfo.setSpan(0L);
                        }
                    }
                } else {
                    if (line.startsWith(skillSpecialiDesc)) {
                        skillLevelInfo.setDescription(line.substring(skillSpecialiDesc.length()));
                    } else if (line.startsWith(skillSpecialiInit)) {
                        if (line.length() != skillSpecialiInit.length()) {
                            skillLevelInfo.setInitialValue(Long.valueOf(line.substring(skillSpecialiInit.length())));
                        } else {
                            skillLevelInfo.setInitialValue(0L);
                        }
                    } else if (line.startsWith(skillSpecialiConsume)) {
                        if (line.length() != skillSpecialiConsume.length()) {
                            skillLevelInfo.setConsumeValue(Long.valueOf(line.substring(skillSpecialiConsume.length())));
                        } else {
                            skillLevelInfo.setConsumeValue(0L);
                        }
                    } else if (line.startsWith(skillSpecialiSpan)) {
                        if (line.length() != skillSpecialiSpan.length()) {
                            skillLevelInfo.setSpan(Long.valueOf(line.substring(skillSpecialiSpan.length())));
                        } else {
                            skillLevelInfo.setSpan(0L);
                        }
                    } else if (line.contains(Constance.REMARKS_EXTRA)) {
                        int idx = line.indexOf(Constance.REMARKS_EXTRA);
                        if (ObjectUtils.isNotEmpty(idx)) {
                            remarks.append(line.substring(idx)).append("\t");
                        }
                    }
                }
            }
            skillLevelInfo.setRemarks(remarks.toString());
            skillLevelInfoList.add(skillLevelInfo);
            // log.info("=====打印实体内容:{}", skillLevelInfo);
        }
        if (!CollectionUtils.isEmpty(skillLevelInfoList)) {
            insertSkillInfo(skillLevelInfoList);
        }
    }

}
