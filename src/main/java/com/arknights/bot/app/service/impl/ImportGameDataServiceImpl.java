package com.arknights.bot.app.service.impl;

import com.arknights.bot.app.service.ImportGameDataService;
import com.arknights.bot.domain.entity.OperatorBaseInfo;
import com.arknights.bot.domain.entity.SkillLevelInfo;
import com.arknights.bot.infra.constant.Constance;
import com.arknights.bot.infra.mapper.ImportGameDataMapper;
import com.arknights.bot.infra.util.RequestMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            case "干员一览":
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
        String name = content;
        try {
            String sEncode = URLEncoder.encode(name, "UTF-8");
            log.info("encode后:" + sEncode);
            name = sEncode;
            String sDecode = URLDecoder.decode(sEncode, "UTF-8");
            log.info("decode后:" + sDecode);
        } catch (
                UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String url = BILI_WIKI_URL + name;
        String result = requestMsgUtil.sendGet(url);

        result = requestMsgUtil.useJsoup(result);
        log.info("result\n,{}", result);

        // 奇怪的截取判定)
        String matchS1 = Constance.OPERATOR_START_LOCAL;
        String matchS2 = Constance.OPERATOR_END_LOCAL;
        Pattern startP = Pattern.compile(matchS1);
        Pattern endP = Pattern.compile(matchS2);


        Matcher matcherStart = startP.matcher(result);
        Matcher matcherEnd = endP.matcher(result);

        int start = 0;
        int end = 0;
        int length = result.length();
        log.info("总长度{}", length);
        // 开始位置
        if (matcherStart.find()) {
            log.info("match start right ");
            start = matcherStart.start();
            // 结束位置
            if (matcherEnd.find()) {
                log.info("match end right ");
                end = matcherEnd.start();
            }
            if (ObjectUtils.isNotEmpty(start) && ObjectUtils.isNotEmpty(end)) {
                log.info("起始下标位置{}，结束下标位置{}", start, end);
                result = result.substring(start + (matchS1.length()), end);
                log.info("按连续空格拆分行");
                String[] strArray = result.split("\\s+");
                List<OperatorBaseInfo> operatorBaseInfoList = new ArrayList<>();
                Long processId = 10L;
                for (String line : strArray) {
                    if (StringUtils.isBlank(line) || StringUtils.equals(line, "异") || StringUtils.equals(line, "活") || StringUtils.equals(line, "限") || StringUtils.equals(line, "升")) {
                        continue;
                    }
                    OperatorBaseInfo operatorBaseInfo = OperatorBaseInfo.builder().operatorId(processId).name(line).build();
                    operatorBaseInfoList.add(operatorBaseInfo);
                    log.info("干员名:{}", line);
                    processId++;
                }
                log.info("统计干员{}名，准备插入", operatorBaseInfoList.size());
                ImportGameDataService inf = (ImportGameDataService) AopContext.currentProxy();
                inf.insertOperatorInfo(operatorBaseInfoList);
            }
        }
    }

    /**
     * 干员技能信息导入
     *
     * @param content
     */
    @Override
    public void skillInfoImport(String content) {

        List<OperatorBaseInfo> operatorBaseInfos = importGameDataMapper.selectOperatorInfo(null);
        for (OperatorBaseInfo item : operatorBaseInfos) {
            String name = "";
            if (StringUtils.isBlank(item.getName())) {
                continue;
            }
            name = item.getName();
            try {
                String sEncode = URLEncoder.encode(name, "UTF-8");
                log.info("encode后:" + sEncode);
                name = sEncode;
                String sDecode = URLDecoder.decode(sEncode, "UTF-8");
                log.info("decode后:" + sDecode);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String url = PRTS_WIKI_URL + "?title=" + name + "&action=edit";
            String result = requestMsgUtil.sendGet(url);

            result = requestMsgUtil.useJsoup(result);
            // log.info("抓取内容去除html格式后:{}\n", result);

            // 截取技能部分
            String matchS1 = "==技能==";
            String matchS2 = "==后勤技能==";
            String matchSkillOne = "'''技能1";
            String matchSkillTwo = "'''技能2";
            String matchSkillThree = "'''技能3";
            Pattern startP = Pattern.compile(matchS1);
            Pattern endP = Pattern.compile(matchS2);
            Pattern skillOne = Pattern.compile(matchSkillOne);
            Pattern skillTwo = Pattern.compile(matchSkillTwo);
            Pattern skillThree = Pattern.compile(matchSkillThree);

            Matcher matcherStart = startP.matcher(result);
            Matcher matcherEnd = endP.matcher(result);
            // 似乎同一对象的find()方法不能重复调用:
            // This method starts at the beginning of this matcher's region,
            // or, if a previous invocation of the method was successful and the matcher has not since been reset,
            // at the first character not matched by the previous match
            // PS:如果同一对象不调用matcher.reset()方法，下次find匹配会从上一次find()成功匹配的结束位置开始

            int start = 0;
            int end = 0;
            String firstSkillContent = "";
            String secondSkillContent = "";
            String thirdSkillContent = "";
            int length = result.length();
            log.info("总长度{}", length);
            // 开始位置
            if (matcherStart.find()) {
                log.info("match start right ");
                start = matcherStart.start();
                // 结束位置
                if (matcherEnd.find()) {
                    log.info("match end right ");
                    end = matcherEnd.start();
                }
                // 拆分最多三个部分，对应三个技能
                if (ObjectUtils.isNotEmpty(start) && ObjectUtils.isNotEmpty(end)) {
                    log.info("起始下标位置{}，结束下标位置{}", start, end);
                    result = result.substring(start + (matchS1.length()), end);
                    // 去除多余字符
                    result = requestMsgUtil.replaceCharacter(result);
                    log.info("初步截取技能部分字符串长度{}", result.length());
                    Matcher matcherSkillOne = skillOne.matcher(result);
                    Matcher matcherSkillTwo = skillTwo.matcher(result);
                    Matcher matcherSkillThree = skillThree.matcher(result);
                    // 抓取第一个技能
                    if (matcherSkillOne.find()) {
                        int firstIndex = 0;
                        int secondIndex = 0;
                        int thirdIndex = 0;
                        firstIndex = matcherSkillOne.start();
                        log.info("匹配到一技能字段开始位置:{}", firstIndex);
                        // 抓取第二个技能
                        if (matcherSkillTwo.find()) {
                            secondIndex = matcherSkillTwo.start();
                            log.info("匹配到二技能字段开始位置{}", secondIndex);
                            firstSkillContent = result.substring(firstIndex + (matchSkillOne.length()), secondIndex);
                            // 抓取第三个技能
                            if (matcherSkillThree.find()) {
                                thirdIndex = matcherSkillThree.start();
                                log.info("匹配到三技能字段开始位置{}", thirdIndex);
                                secondSkillContent = result.substring(secondIndex + (matchSkillTwo.length()), thirdIndex);
                                thirdSkillContent = result.substring(thirdIndex + (matchSkillThree.length()), result.length() - 1);
                            } else {
                                secondSkillContent = result.substring(secondIndex + (matchSkillTwo.length()), result.length() - 1);
                            }
                        }
                    } else {
                        log.error("抓取技能位置失败");
                    }


                    String s1 = requestMsgUtil.useJsoup(firstSkillContent);
                    String s2 = requestMsgUtil.useJsoup(secondSkillContent);
                    String s3 = requestMsgUtil.useJsoup(thirdSkillContent);

                    log.info("开始字符串拆分与数据插入");
                    if (StringUtils.isNotBlank(s1)) {
                        handleSkillInfo(s1, 1L);
                        log.info("技能一插入完成");
                    } else {
                        log.error("技能拆分异常，当前为空");
                    }
                    if (StringUtils.isNotBlank(s2)) {
                        handleSkillInfo(s2, 2L);
                        log.info("技能二插入完成");
                    }
                    if (StringUtils.isNotBlank(s3)) {
                        handleSkillInfo(s3, 3L);
                        log.info("技能三插入完成");
                    }

                }
            }
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insertOperatorInfo(List<OperatorBaseInfo> operatorBaseInfoList) {
        for (OperatorBaseInfo line : operatorBaseInfoList) {
            importGameDataMapper.insertOperatorBaseInfo(line);
        }
    }

    @Override
    public void handleSkillInfo(String result, Long order) {
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
                skillLevelInfo.setOrder(order);
                // 技能等级
                skillLevelInfo.setLevel((long) i);
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
                skillLevelInfoList.add(skillLevelInfo);
            }
            skillLevelInfo.setRemarks(remarks.toString());
            // log.info("=====打印实体内容:{}", skillLevelInfo);
        }
        if (!CollectionUtils.isEmpty(skillLevelInfoList)) {
            insertSkillInfo(skillLevelInfoList);
        }
    }

    @Override
    public void insertSkillInfo(List<SkillLevelInfo> skillLevelInfoList) {
        for (SkillLevelInfo skillLevelInfo : skillLevelInfoList) {
            importGameDataMapper.insertSkillInfo(skillLevelInfo);
        }
    }


}
