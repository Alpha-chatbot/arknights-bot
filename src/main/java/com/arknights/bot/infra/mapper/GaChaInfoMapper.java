package com.arknights.bot.infra.mapper;

import com.arknights.bot.domain.entity.GaChaInfo;

import java.util.List;

/**
 * Created by wangzhen on 2023/1/23 16:01
 * @author 14869
 */
public interface GaChaInfoMapper {

    /**
     * 插入寻访记录
     * @param gaChaInfo
     * @return
     */
    Integer insertGaChaInfo(GaChaInfo gaChaInfo);

    /**
     * 查询本批次数据
     * @param processId
     * @return
     */
    List<GaChaInfo> selectGaChaByProcessId(Long processId);
}
