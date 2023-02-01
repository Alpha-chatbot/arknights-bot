package com.arknights.bot.app.service.impl;

import com.arknights.bot.app.service.ImportGameDataService;
import com.arknights.bot.domain.entity.OperatorBaseInfo;
import com.arknights.bot.infra.constant.Constance;
import com.arknights.bot.infra.util.RequestMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wangzhen on 2023/2/1 18:00
 * @author 14869
 */
@Slf4j
@Service
public class ImportGameDataServiceImpl implements ImportGameDataService {

    private static final String BILI_WIKI_URL = "https://wiki.biligame.com/arknights/";

    @Autowired
    private RequestMsgUtil requestMsgUtil;

    @Override
    public void OperatorBaseInfoImport(String content) {
        String name = "";
        try {
            String sEncode = URLEncoder.encode("干员一览", "UTF-8");
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
                for (String line : strArray) {
                    if(StringUtils.equals(line, "异") || StringUtils.equals(line, "活") || StringUtils.equals(line, "限") || StringUtils.equals(line, "升")){
                        continue;
                    }
                    Calendar now = Calendar.getInstance();
                    Long processId = now.getTime().getTime();
                    OperatorBaseInfo operatorBaseInfo = OperatorBaseInfo.builder().operatorId(processId).name(line).build();
                    operatorBaseInfoList.add(operatorBaseInfo);
                    log.info("干员名:{}",line);
                }
                log.info("插入干员{}名，操作完成", operatorBaseInfoList.size());
            }
    }



}
}
