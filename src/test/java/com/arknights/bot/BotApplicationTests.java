package com.arknights.bot;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
public class BotApplicationTests {

    @Test
    public void contextLoads() {
    }

    @Test
    public void testSplit(){
        String text = "## 阿米驴~001 ##11";
        String[] a = text.split("\001");
        System.out.println(a[0]);
    }

    /**
     * 时间戳转换
     */
    @Test
    public void testTs(){
        // convertToTimestamp();
        conversionTime("1674440758");
        System.out.println(48/10);
    }

    public void convertToTimestamp(String time) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            long timestamp = cal.getTimeInMillis();
            System.out.println("10位时间戳="+timestamp);
            System.out.println("13位时间戳"+timestamp/1000);
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
        System.out.println("time:"+time);
        return time;
    }

    @Test
    public void testEnter(){
        // 输出 阿瓦达啃大瓜
        System.out.println("测试回车换行"+"\r"+"阿瓦达啃大瓜");
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



}
