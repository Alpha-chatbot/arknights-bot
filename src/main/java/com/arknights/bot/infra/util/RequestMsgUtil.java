package com.arknights.bot.infra.util;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抓取数据工具类（笨方法，其实并不是很想用）
 * Created by wangzhen on 2023/2/1 17:53
 * @author 14869
 */
@Slf4j
@Component
public class RequestMsgUtil {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36";


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

    /**
     * html格式处理
     * @param htmlContent
     * @return
     */
    public String useJsoup(String htmlContent) {
        String content = "";
        if (StringUtils.isNotBlank(htmlContent)) {
            //方法1. clean方法
            // content = Jsoup.clean(htmlContent, Whitelist.none());
            // log.info("clean结果：" + content);
            //方法2. parse方法后通过text获取内容
            content = Jsoup.parse(htmlContent).text();

        }
        return content;
    }

    /**
     * 剔除多余字符
     * @param str
     * @return
     */
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

    public String replace2Int(String result){
        String regexInt = "(\\.0)|(\\.)";
        result = result.replaceAll(regexInt, "");
        return result;
    }


}
