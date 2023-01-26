package com.arknights.bot.app.service;

import com.arknights.bot.api.dto.PoolsInfoDto;

import java.util.List;

/**
 * Created by wangzhen on 2023/1/23 10:14
 * @author 14869
 */
public interface GaChaInfoService {

    /**
     * 获取查询必需的token
     */
    void getToken();

    /**
     * 寻访分页查询
     */
    String gaChaQueryByPage(int page, String token, Long qq);

    /**
     * 寻访记录数据插入
     * @param poolsInfoDtos
     */
    void insertGaChaInfo(List<PoolsInfoDto> poolsInfoDtos, Long processId, Long qq);
}
