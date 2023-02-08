package com.arknights.bot;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arknights.bot.app.service.ImportGameDataService;
import com.arknights.bot.app.service.impl.ImportGameDataServiceImpl;
import com.arknights.bot.domain.entity.OperatorBaseInfo;
import com.arknights.bot.domain.entity.SkillLevelInfo;
import com.arknights.bot.infra.constant.Constance;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// @RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class BotApplicationTests {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36";
    private final static String FILE_PATH_PREFIX = "/zoe/arknights-bot/data";

    @Test
    public void contextLoads() {
    }

    @Test
    public void testSplit() {
        String text = "## 阿米驴~001 ##11";
        String[] a = text.split("\001");
        System.out.println(a[0]);
    }

    /**
     * 时间戳转换
     */
    @Test
    public void testTs() {
        // convertToTimestamp();
        conversionTime("1674440758");
        System.out.println(48 / 10);
    }

    public void convertToTimestamp(String time) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            long timestamp = cal.getTimeInMillis();
            System.out.println("10位时间戳=" + timestamp);
            System.out.println("13位时间戳" + timestamp / 1000);
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    //传入时间戳即可
    public String conversionTime(String timeStamp) {
        //yyyy-MM-dd HH:mm:ss 转换的时间格式  可以自定义
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //转换
        String time = sdf.format(new Date(Long.parseLong(timeStamp)));
        System.out.println("time:" + time);
        return time;
    }

    @Test
    public void testEnter() {
        // 输出 阿瓦达啃大瓜
        System.out.println("测试回车换行" + "\r" + "阿瓦达啃大瓜");
        // 取消文本中的换行
        System.out.println(replaceEnter("从前有一只黄皮耗子\n" +
                "叫皮卡丘\r" +
                "神奇的是从前有一只蒜头王八"));
    }

    public static String replaceEnter(String str) {
        String reg = "[\n-\r]";
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(str);
        return m.replaceAll(",");
    }


    public String replaceCharacter(String str) {

        // 处理字符串中的无用字符
        // 匹配color格式 和6位数字字母
        Pattern pattern1 = Pattern.compile("(\\{[\\+\\-\\*]\\|[0-9]{1,3}[%]*\\|\\{\\{color\\|#[0-9a-zA-Z]{6}\\|)");
        Pattern pattern2 = Pattern.compile("(\\{\\{color\\|#[0-9a-zA-Z]{6}\\|)");
        // 移除术语标识
        Pattern pattern3 = Pattern.compile("(\\|ba.dying\\|)");
        Matcher matcher1 = pattern1.matcher(str);
        int count = 0;
        while (matcher1.find()) {
            count++;
        }
        // 统一替换
        String result = matcher1.replaceAll("");
        // log.info("第一次替换后:{}",result);
        Matcher matcher2 = pattern2.matcher(result);
        while (matcher2.find()) {
            count++;
        }
        result = matcher2.replaceAll("");
        Matcher matcher3 = pattern3.matcher(result);
        while (matcher3.find()) {
            count++;
        }
        result = matcher3.replaceAll(":");
        String regex = "(\\{)|(\\}|(\\|))";
        result = result.replaceAll(regex, "");
        // log.info("第三次替换后:{}", result);
        return result;
    }


    @Test
    public void testGetResponseInfo() {

        // url编码：对于中文,模仿HTML源码中字符集设定进行转换,可能是 UTF-8 或GBK 等
        String name = "";
        try {
            String sEncode = URLEncoder.encode("伺夜", "UTF-8");
            System.out.println("encode后:" + sEncode);
            name = sEncode;
            String sDecode = URLDecoder.decode(sEncode, "UTF-8");
            System.out.println("decode后:" + sDecode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // String url = "https://prts.wiki/w/" + name;
        // https://prts.wiki/index.php?title=%E5%B5%AF%E5%B3%A8&action=edit
        String url = "https://prts.wiki/index.php" + "?title=" + name + "&action=edit";
        // String url = "https://prts.wiki/index.php?" + "title=" + name + "action=edit";
        String result = sendGet(url);
        // <span id="技能"> 与  span id="后勤技能 之间 每个 <tbody> 对应一个技能

        result = useJsoup(result);
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
                result = replaceCharacter(result);
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
                    log.error("抓取技能失败");
                }


                String s1 = useJsoup(firstSkillContent);
                String s2 = useJsoup(secondSkillContent);
                String s3 = useJsoup(thirdSkillContent);

                log.info("开始字符串拆分与数据插入");
                if (StringUtils.isNotBlank(s1)) {
                    insertSkillInfo(s1, 1);
                    log.info("技能一插入完成");
                } else {
                    log.error("技能拆分异常，当前为空");
                }
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (StringUtils.isNotBlank(s2)) {
                    insertSkillInfo(s2, 2);
                    log.info("技能二插入完成");
                }
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (StringUtils.isNotBlank(s3)) {
                    insertSkillInfo(s3, 3);
                    log.info("技能三插入完成");
                }


            }
        }

    }

    public void insertSkillInfo(String result, Integer order) {
        log.info("按连续空格拆分行");
        String[] strArray = result.split("\\s+");
        if (ObjectUtils.isEmpty(strArray) || strArray.length < 30) {
            log.info("解析异常，技能资料不全");
            return;
        }

        // demo 临时测试
        for(String line : strArray){
            System.out.println("=====");
            System.out.println(line);
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
        for (int i = 1; i <= 10; i++) {
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
            System.out.println("=====");
            log.info("打印实体内容:{}", skillLevelInfo);
        }


    }


    public String useJsoup(String htmlContent) {
        String content = "";
        if (StringUtils.isNotBlank(htmlContent)) {
            //1. clean方法
            // content = Jsoup.clean(htmlContent, Whitelist.none());
            // log.info("clean结果：" + content);
            //2. parse方法后通过text获取内容
            content = Jsoup.parse(htmlContent).text();

        }
        return content;
    }

    public String sendGet(String url) {
        //1.生成httpclient，相当于该打开一个浏览器
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //设置请求和传输超时时间
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
        CloseableHttpResponse response = null;
        String html = null;
        //2.创建get请求，相当于在浏览器地址栏输入 网址
        HttpGet request = new HttpGet(url);
        try {
            request.setHeader("User-Agent", USER_AGENT);
            request.setConfig(requestConfig);
            //3.执行get请求，相当于在输入地址栏后敲回车键
            response = httpClient.execute(request);
            //4.判断响应状态为200，进行处理
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                //5.获取响应内容
                HttpEntity httpEntity = response.getEntity();
                html = EntityUtils.toString(httpEntity, "UTF-8");
            } else {
                //如果返回状态不是200，比如404（页面不存在）等，根据情况做处理，这里略
                System.out.println("返回状态不是200");
                // System.out.println(EntityUtils.toString(response.getEntity(), "utf-8"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //6.关闭
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);
        }
        return html;
    }

    @Test
    public void saad() {

        // 攻击力<@ba.vup>+{atk:0%}</>，防御力<@ba.vup>+{def:0%}</>，每次攻击额外造成相当于攻击力<@ba.vup>{attack@blemsh_s_3_extra_dmg[magic].atk_scale:0%}</>的法术伤害，并恢复周围一名<@ba.rem>其他</>友方单位相当于攻击力<@ba.vup>{heal_scale:0%}</>的生命
        String str = "攻击力<@ba.vup>+{atk:0%}</>，天赋的触发几率提升至<@ba.vup>{talent_scale:0.0}</>倍";
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

        log.info("看这次替换后:{}", result);
        // 有特殊情况会出现xxx:0.0%，需要替换为xx0%
        String regexSpec = "(:0\\.0%)";
        result = result.replaceAll(regexSpec, "0%");
        String regexTimes = "(:0\\.0})";
        result = result.replaceAll(regexTimes, "");
        String regex = "(\\{)|(\\}|(\\|)|(:))";
        result = result.replaceAll(regex, "");
        log.info("首次替换后:{}", result);
        String regexP = "(<\\$ba\\.[a-z]{1,9}>)|(<@ba\\.[a-z]{1,9}>)|(<\\$ba\\.[a-z]{1,9}\\.[a-z]{1,9}>)";
        result = result.replaceAll(regexP, "");
        // 这里不能用replaceAll，因为用了的话不添加转义字符会无法识别替换，而且每个变量只出现一次，用replace即可
        result = result.replace("atk", "0.4");
        result = result.replace("ep_heal_ratio", "0.6");
        result = result.replace("cnt", "1.0");
        result = result.replace("talent_scale", "3.0");
        result = result.replace("hp_recovery_per_sec_by_max_hp_ratio", "0.03");
        result = result.replace("hp_recovery_per_sec_by_max_hp_ratio".toUpperCase(), "0.03");

        log.info("当前:{}", result);
        String value = "3.0";
        float v = Float.parseFloat(value);
        v = v*100;

        log.info("当前v:{}", (long)v);
        // 在这里处理百分号转换问题，比如现在格式是 攻击力+0.40% ,替换为 40%
        String regexPercent = "[0-9][.][0-9]{1,2}0%";
        result = result.replaceAll(regexPercent, String.valueOf((long)v)+"%");
        // result = result.replace("heal_scale", "0.6");
        // <$ba.dt.element>

        log.info("处理小数点和百分号前:{}", result);
        // 更改格式
/*        String regexInt = "(0%)";
        result = result.replaceAll(regexInt, "");
        log.info("三次替换后:{}", result);*/
    }

    //把一个文件中的内容读取成一个String字符串
    public static String getStr(File jsonFile){
        String jsonStr = "";
        try {
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(jsonFile),"utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Test
    public void zoeTestt(){
        String uploadFileSavePath = "F:\\backup";
        // String uploadFileSavePath = FILE_PATH_PREFIX;
        // character_table.json  skill_table.json

        // 下载文件进行解析
        // File downloadFile = new File(uploadFileSavePath + File.separator + "character_table.json");
        File jsonFile = new File(uploadFileSavePath + File.separator + "character_table.json");
        //通过上面那个方法获取json文件的内容
        String jsonData = getStr(jsonFile);
        //转json对象
        JSONObject parse = (JSONObject)JSONObject.parse(jsonData);
        //获取主要数据
        //遍历key和value
        int i = 0;
        for (Map.Entry<String, Object> entry : parse.entrySet()) {
            // log.info("这条JSON的Key是："+entry.getKey());
            // log.info("这条JSON的Value是："+ entry.getValue());
            String key = entry.getKey();
            JSONObject valueObject =(JSONObject)entry.getValue();
            // 干员中文名称
            String zh_name = valueObject.getString("name");
            // 干员英文名
            String en_name = valueObject.getString("appellation");
            // 转为小写方便匹配其余json信息
            en_name = en_name.toLowerCase();
            // 招聘合同
            String itemUsage = valueObject.getString("itemUsage");
            // 潜能物id，这里用于排除道具类的数据
            String potentialItemId = valueObject.getString("potentialItemId");
            if(StringUtils.isBlank(potentialItemId)){
                continue;
            }

            // 获取技能id,通过skillId去匹配skill_table
            JSONArray skillArr = valueObject.getJSONArray("skills");
            int order = 1;
            for (int j = 0; j < skillArr.size(); j++) {
                JSONObject jsonObject = skillArr.getJSONObject(j);
                String skillId = jsonObject.getString("skillId");
                // 解锁条件
                JSONObject unlockCond = jsonObject.getJSONObject("unlockCond");
                // 0表示精英0开放
                String openLevel = unlockCond.getString("phase");

                log.info("技能信息: 技能id:{}, 技能解锁条件:{}, 技能序号:{}", skillId, openLevel, order);
                order++;
            }
            i++;
            log.info("干员基本信息: key值:{},干员中文名:{}, 干员英文名:{}, 招聘合同:{}", key, zh_name, en_name, itemUsage);
            // 吐槽:为什么道具也会放在人物表里...
        }
        log.info("当前干员总数:{}", i);

    }

    @Test
    public void parseTest(){
        String duration = "-1.0";
        float l = Float.parseFloat(duration);
        if (l < 0) {
            duration = "0";
        }
        log.info("输出:{}", duration);
        duration = "20.0";
        float temp = Float.parseFloat(duration);
        long ss = (long) temp;
        log.info("输出：{}", ss);


        List<String> listTest = Arrays.asList("111", "2", "3333", "44") ;
        listTest.sort((o1, o2) -> {
            if(o1.length() > o2.length()){

                //这里注意，比较o1与o2的大小，若o1 大于 o2 默认会返回 1
                //但是sort排序，默认的是升序排序，所以重写的时候将值改写返回-1，就会变成倒叙排序

                return -1;
            }else if(o1.length() == o2.length()){
                return 0;
            }else {
                return 1;
            }
        });


    }


}
