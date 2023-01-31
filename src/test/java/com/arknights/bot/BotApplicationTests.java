package com.arknights.bot;

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
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@SpringBootTest
public class BotApplicationTests {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36";


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
        return m.replaceAll("");
    }



    public String replaceCharacter(String str) {

        // 处理字符串中的无用字符
        // 匹配color格式 和6位数字字母
        Pattern pattern1 = Pattern.compile("(color\\|#[0-9a-zA-Z]{6}\\|)");
        //
        Pattern pattern2 = Pattern.compile("(\\|ba.dying\\|)");
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
        result = matcher2.replaceAll(":");
        // log.info("第二次替换后:{}", result);
        String regex = "(\\{)|(\\}|(\\|))";
        result = result.replaceAll(regex, "");
        // log.info("第三次替换后:{}", result);
        return  result;
    }


    @Test
    public void testGetResponseInfo() {

        // url编码：对于中文,模仿HTML源码中字符集设定进行转换,可能是 UTF-8 或GBK 等
        String name = "";
        try {
            String sEncode = URLEncoder.encode("嵯峨", "UTF-8");
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
            if(ObjectUtils.isNotEmpty(start) && ObjectUtils.isNotEmpty(end)){
                log.info("起始下标位置{}，结束下标位置{}", start, end);
                result = result.substring(start + (matchS1.length()), end);
                // 去除多余注释
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
                    if(matcherSkillTwo.find()){
                        secondIndex = matcherSkillTwo.start();
                        log.info("匹配到二技能字段开始位置{}", secondIndex);
                        firstSkillContent = result.substring(firstIndex + (matchSkillOne.length()), secondIndex);
                        // 抓取第三个技能
                        if(matcherSkillThree.find()){
                            thirdIndex = matcherSkillThree.start();
                            log.info("匹配到三技能字段开始位置{}", thirdIndex);
                            secondSkillContent = result.substring(secondIndex + (matchSkillTwo.length()), thirdIndex);
                            thirdSkillContent = result.substring(thirdIndex + (matchSkillThree.length()), result.length()-1);
                        } else {
                            secondSkillContent = result.substring(secondIndex + (matchSkillTwo.length()), result.length()-1);
                        }
                    }
                } else {
                    log.error("抓取技能失败");
                }

/*                log.info("技能一:\n" + firstSkillContent);
                log.info("技能二:\n" + secondSkillContent);
                log.info("技能三:\n" + thirdSkillContent);*/

                String s1 = useJsoup(firstSkillContent);
                String s2 = useJsoup(secondSkillContent);
                String s3 = useJsoup(thirdSkillContent);
                if(StringUtils.isNotBlank(s1)){
                    insertSkillInfo(s1, 1);
                    log.info("技能一插入完成");
                }else {
                    log.error("技能拆分异常，当前为空");
                }
                if(StringUtils.isNotBlank(s2)) {
                    insertSkillInfo(s2, 2);
                    log.info("技能二插入完成");
                }
                if(StringUtils.isNotBlank(s3)) {
                    insertSkillInfo(s3, 3);
                    log.info("技能三插入完成");
                }

            }
        }

    }

    public void insertSkillInfo(String result, int order){
        log.info("按连续空格拆分行");
        String [] strArray = result.split("\\s+");
                if(ObjectUtils.isEmpty(strArray) || strArray.length < 30){
                    log.info("解析异常，技能资料不全");
                    return;
                }
                String openLevel = strArray[0];
                String skillName = strArray[2];
                if(skillName.startsWith(Constance.SKILL_NAME_CH)){
                    skillName = skillName.substring(Constance.SKILL_NAME_CH.length());
                } else {
                    log.info("解析异常，技能匹配错位");
                    return;
                }

                int i = 0;
        for(String line: strArray){
            System.out.println("=====");
            SkillLevelInfo skillLevelInfo = new SkillLevelInfo();
            // 技力类型和触发类型
            if(line.startsWith(Constance.SKILL_TYPE_ONE)){
                skillLevelInfo.setPowerType(line.substring(Constance.SKILL_TYPE_ONE.length()));
            } else if(line.startsWith(Constance.SKILL_TYPE_TWO)){
                skillLevelInfo.setTriggerType(line.substring(Constance.SKILL_TYPE_TWO.length()));
            }
            // 每个技能等级对应的描述，初始值，消耗，持续时间
            if(i != 0) {
                String skillDesc = Constance.SKILL + i + Constance.SKILL_DESC;
                String skillInit = Constance.SKILL + i + Constance.SKILL_INIT;
                String skillConsume = Constance.SKILL + i + Constance.SKILL_CONSUME;
                if (line.startsWith(skillDesc)) {
                    skillLevelInfo.setDescription(line.substring(skillDesc.length()));
                } else if(line.startsWith(skillInit)){
                    skillLevelInfo.setInitialValue(Long.valueOf(line.substring(skillInit.length())));
                } else if(line.startsWith(skillConsume)){
                    skillLevelInfo.setConsumeValue(Long.valueOf(line.substring(skillConsume.length())));
                }
            }
            String powerType = strArray[2].substring(0, 4);
            String triggerType = strArray[3].substring(0, 4);
            String level = strArray[4];
            String desc = strArray[5];

            String lv1 = strArray[9];
            String description1 = "";
            if(lv1.equals("1")){

            }
            System.out.println(line);
            i++;
        }

    }


    public String useJsoup( String htmlContent){
        String content = "";
        if(StringUtils.isNotBlank(htmlContent)) {
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


}
