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
import com.arknights.bot.infra.mapper.GaChaInfoMapper;
import lombok.extern.slf4j.Slf4j;
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
        String firstContent = sendGet(firstPage);

        log.info("get首次请求返回内容{}", firstContent);
        // json处理
        List<PoolsInfoDto> poolsInfoDtoList = json2Entity(firstContent);
        if (CollectionUtils.isEmpty(poolsInfoDtoList)) {
            return null;
        }
        PoolsInfoDto poolsInfoDto = poolsInfoDtoList.get(0);
        // 获得总页数
        int total = poolsInfoDto.getPageRequest().getTotal();
        log.info("返回总条数{}", total);
        if (total > Constance.PAGE_SIZE) {
            int pages = (total / 10);

            for (int i = 0; i < pages + 1; i++) {
                // 从第二页开始查询
                int counts = i + 2;
                log.info("第{}页内容", counts);
                String url = URL + "?page=" + counts + "&token=" + token + "&channelId=1";
                log.info("url:{}", url);
                String content = sendGet(url);
                List<PoolsInfoDto> resultList = json2Entity(content);
                if (!CollectionUtils.isEmpty(resultList)) {
                    poolsInfoDtoList.addAll(resultList);
                }
                log.info("当前为第{}页，每页最多十条记录", i + 2);
                resultList.clear();
            }
        }
        // 数据写表
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
            // 单抽
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
                // 十连寻访,这里入参ts都是相同的，只能手动变动,寻访时间字段相同
                log.info("十连寻访:{}", item.getChars());
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
     * get请求 爬取
     *
     * @param url
     * @return
     */
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
                log.info("返回状态不是200");
                log.info(EntityUtils.toString(response.getEntity(), "utf-8"));
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

    public List<PoolsInfoDto> json2Entity(String result) {

        List<PoolsInfoDto> poolsInfoDtoList = new ArrayList<>();
        //获得json对象，此处是com.alibaba.fastjson
        JSONObject jsonObject = JSON.parseObject(result);
        //获得json对象
        JSONObject results = jsonObject.getJSONObject("data");
        //获得code字段
        String code = jsonObject.getString("code");
        String message = jsonObject.getString("message");

        // 获取分页信息
        JSONObject pageInfo = results.getJSONObject("pagination");
        if (Objects.isNull(pageInfo)) {
            return Collections.emptyList();
        }
        // json转为实体类
        PageRequest pageRequest = JSON.toJavaObject(pageInfo, PageRequest.class);

        // 卡池信息,json数组 ,后续用get(x)可以获得json对象
        JSONArray array = results.getJSONArray("list");
        for (int i = 0; i < array.size(); i++) {
            // 遍历 jsonarray 数组，把每一个对象转成 json 对象
            JSONObject job = array.getJSONObject(i);
            // json转为卡池实体类
            PoolsInfoDto poolsInfoDto = JSON.toJavaObject(job, PoolsInfoDto.class);
            // 时间戳转换, 鹰角的是10位秒级时间戳
            Long secondTs = poolsInfoDto.getTs();
            String gachaTime;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //将时间戳转换为时间
            Date date = new Date(secondTs*1000L);
            //将时间调整为yyyy-MM-dd HH:mm:ss时间样式
            gachaTime = simpleDateFormat.format(date);
            poolsInfoDto.setGachaTime(gachaTime);

            // 添加分页信息
            poolsInfoDto.setPageRequest(pageRequest);
            log.info("当前页第{}条的poolsInfo:{}", i+1 ,poolsInfoDto);
            poolsInfoDtoList.add(poolsInfoDto);
        }

        return poolsInfoDtoList;
    }

    public Long getProcessId() {
        Calendar now = Calendar.getInstance();
        return now.getTime().getTime();
    }
}
