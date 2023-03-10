package com.arknights.bot.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arknights.bot.api.dto.EmployeesDto;
import com.arknights.bot.api.dto.PoolsInfoDto;
import com.arknights.bot.app.service.GaChaInfoService;
import com.arknights.bot.domain.entity.GaChaInfo;
import com.arknights.bot.domain.entity.PageRequest;
import com.arknights.bot.infra.constant.Constance;
import com.arknights.bot.infra.constant.UserAgentConstance;
import com.arknights.bot.infra.mapper.GaChaInfoMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by wangzhen on 2023/1/23 10:14
 *
 * @author 14869
 */
@Slf4j
@Service
public class GaChaInfoServiceImpl implements GaChaInfoService {


    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36";
    private static final String URL = "https://ak.hypergryph.com/user/api/inquiry/gacha";

    @Resource
    private GaChaInfoMapper gaChaInfoMapper;

    @Override
    public void getToken() {

    }

    @Override
    public String gaChaQueryByPage(int page, String token, Long qq) {
        // https://ak.hypergryph.com/user/api/inquiry/gacha?page=1&token=l8jK5JGHe8SN25PI91kgc7xb&channelId=1
        String firstPage = URL + "?page=1" + "&token=" + token + "&channelId=1";
        // ????????????????????????????????????
        String userAgent = testConn(firstPage);
        if (StringUtils.isEmpty(userAgent)) {
            return null;
        }
        String firstContent = sendGet(firstPage, userAgent);

        log.info("get????????????????????????{}", firstContent);

        // json??????
        List<PoolsInfoDto> poolsInfoDtoList = json2Entity(firstContent);
        if (CollectionUtils.isEmpty(poolsInfoDtoList)) {
            return null;
        }
        PoolsInfoDto poolsInfoDto = poolsInfoDtoList.get(0);
        // ???????????????
        int total = poolsInfoDto.getPageRequest().getTotal();
        log.info("???????????????{}", total);
        if (total > Constance.PAGE_SIZE) {
            int pages = (total / 10);

            for (int i = 0; i < pages + 1; i++) {
                // ????????????????????????
                int counts = i + 2;
                log.info("???{}?????????", counts);
                String url = URL + "?page=" + counts + "&token=" + token + "&channelId=1";
                log.info("url:{}", url);
                String content = sendGet(url, userAgent);
                List<PoolsInfoDto> resultList = json2Entity(content);
                if (!CollectionUtils.isEmpty(resultList)) {
                    poolsInfoDtoList.addAll(resultList);
                }
                log.info("????????????{}??????????????????????????????", i + 2);
                resultList.clear();
            }
        }
        // ????????????
        if (!CollectionUtils.isEmpty(poolsInfoDtoList)) {
            Long processId;
            try {
                processId = getProcessId();
                insertGaChaInfo(poolsInfoDtoList, processId, qq);
            } catch (Exception e) {
                log.info(e.toString());
                return Constance.STATUS_ERROR + e.toString();
            }
            return String.valueOf(processId);
        }

        return null;
    }

    @Override
    public void insertGaChaInfo(List<PoolsInfoDto> poolsInfoDtos, Long processId, Long qq) {
        List<GaChaInfo> gaChaInfoList = new ArrayList<>();
        for (PoolsInfoDto item : poolsInfoDtos) {
            Long ts = item.getTs();
            String pool = item.getPool();
            String gachaTime = item.getGachaTime();
            // ??????
            if (item.getChars().size() == 1) {
                GaChaInfo gaChaInfo = new GaChaInfo();
                EmployeesDto employeesDto = item.getChars().get(0);
                gaChaInfo.setIsNew(employeesDto.getIsNew());
                gaChaInfo.setOperatorsName(employeesDto.getName());
                gaChaInfo.setRarity(employeesDto.getRarity() + 1);
                gaChaInfo.setTs(ts);
                gaChaInfo.setPool(pool);
                gaChaInfo.setProcessId(processId);
                gaChaInfo.setQq(qq);
                gaChaInfo.setGachaTime(gachaTime);
                gaChaInfoList.add(gaChaInfo);
            } else if (item.getChars().size() > 1) {
                // ????????????,????????????ts????????????????????????????????????,????????????????????????
                log.info("????????????:{}", item.getChars());
                List<EmployeesDto> chars = item.getChars();
                GaChaInfo gaChaInfo;
                for (EmployeesDto dto : chars) {
                    gaChaInfo = new GaChaInfo();
                    gaChaInfo.setIsNew(dto.getIsNew());
                    gaChaInfo.setOperatorsName(dto.getName());
                    gaChaInfo.setRarity(dto.getRarity() + 1);
                    gaChaInfo.setTs(++ts);
                    gaChaInfo.setPool(pool);
                    gaChaInfo.setProcessId(processId);
                    gaChaInfo.setQq(qq);
                    gaChaInfo.setGachaTime(gachaTime);
                    gaChaInfoList.add(gaChaInfo);
                }
                chars.clear();
            }
        }

        // insert
        for (GaChaInfo info : gaChaInfoList) {
            gaChaInfoMapper.insertGaChaInfo(info);
        }
    }


    /**
     * get?????? ??????
     *
     * @param url
     * @return
     */
    public String sendGet(String url, String userAgent) {
        //1.??????httpclient????????????????????????????????????
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //?????????????????????????????????
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
        CloseableHttpResponse response = null;
        String html = null;
        //2.??????get????????????????????????????????????????????? ??????
        HttpGet request = new HttpGet(url);
        try {
            request.setHeader("User-Agent", userAgent);
            request.setConfig(requestConfig);
            //3.??????get???????????????????????????????????????????????????
            response = httpClient.execute(request);
            //4.?????????????????????200???????????????
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                //5.??????????????????
                HttpEntity httpEntity = response.getEntity();
                html = EntityUtils.toString(httpEntity, "UTF-8");
            } else {
                //????????????????????????200?????????404????????????????????????????????????????????????????????????
                log.info("??????????????????200");
                log.info(EntityUtils.toString(response.getEntity(), "utf-8"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //6.??????
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);
        }
        return html;
    }

    public List<PoolsInfoDto> json2Entity(String result) {

        List<PoolsInfoDto> poolsInfoDtoList = new ArrayList<>();
        //??????json??????????????????com.alibaba.fastjson
        JSONObject jsonObject = JSON.parseObject(result);
        //??????json??????
        JSONObject results = jsonObject.getJSONObject("data");
        //??????code??????
        String code = jsonObject.getString("code");
        String message = jsonObject.getString("message");

        // ??????????????????
        JSONObject pageInfo = results.getJSONObject("pagination");
        if (Objects.isNull(pageInfo)) {
            return Collections.emptyList();
        }
        // json???????????????
        PageRequest pageRequest = JSON.toJavaObject(pageInfo, PageRequest.class);

        // ????????????,json?????? ,?????????get(x)????????????json??????
        JSONArray array = results.getJSONArray("list");
        for (int i = 0; i < array.size(); i++) {
            // ?????? jsonarray ????????????????????????????????? json ??????
            JSONObject job = array.getJSONObject(i);
            // json?????????????????????
            PoolsInfoDto poolsInfoDto = JSON.toJavaObject(job, PoolsInfoDto.class);
            // ???????????????, ????????????10??????????????????
            Long secondTs = poolsInfoDto.getTs();
            String gachaTime;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //???????????????????????????
            Date date = new Date(secondTs * 1000L);
            //??????????????????yyyy-MM-dd HH:mm:ss????????????
            gachaTime = simpleDateFormat.format(date);
            poolsInfoDto.setGachaTime(gachaTime);

            // ??????????????????
            poolsInfoDto.setPageRequest(pageRequest);
            log.info("????????????{}??????poolsInfo:{}", i + 1, poolsInfoDto);
            poolsInfoDtoList.add(poolsInfoDto);
        }

        return poolsInfoDtoList;
    }

    public Long getProcessId() {
        Calendar now = Calendar.getInstance();
        return now.getTime().getTime();
    }

    public String testConn(String firstPage) {

        // ??????????????????
        String agent = UserAgentConstance.GOOGLE_BROWSER;
        String firstContent = sendGet(firstPage, agent);
        // ??????????????????: {"code":3000,"msg":"????????????"}
        JSONObject jsonObject = JSON.parseObject(firstContent);
        //??????code??????,??????0????????????????????????
        String code = jsonObject.getString("code");
        if (!Constance.ZERO.equals(code)) {
            for (int i = 0; i < 6; i++) {

                if (i == 0) {
                    agent = UserAgentConstance.QQ_BROWSER;
                    firstContent = sendGet(firstPage, agent);
                } else if (i == 1) {
                    agent = UserAgentConstance.FOX_BROWSER;
                    firstContent = sendGet(firstPage, agent);
                } else if (i == 2) {
                    agent = UserAgentConstance.EDGE_BROWSER;
                    firstContent = sendGet(firstPage, agent);
                } else if (i == 3) {
                    agent = UserAgentConstance.SG_BROWSER;
                    firstContent = sendGet(firstPage, agent);
                } else if (i == 4) {
                    agent = UserAgentConstance.N360_BROWSER;
                    firstContent = sendGet(firstPage, agent);
                } else {
                    agent = UserAgentConstance.LBB_BROWSER;
                    firstContent = sendGet(firstPage, agent);
                }
                jsonObject = JSON.parseObject(firstContent);
                //??????code??????
                code = jsonObject.getString("code");
                log.info("get????????????????????????{}", firstContent);
                log.info("???????????????{}, ??????code???{}", agent, code);
                if (Constance.ZERO.equals(code)) {
                    log.info("????????????????????????????????????{}", agent);
                    break;
                }
            }
            if (!Constance.ZERO.equals(code)) {
                return null;
            }
        }

        return agent;
    }

}
