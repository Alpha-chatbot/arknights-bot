package com.arknights.bot.app.service.impl;

import com.arknights.bot.api.dto.PoolsInfoDto;
import com.arknights.bot.app.service.GaChaInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by wangzhen on 2023/1/23 10:14
 *
 * @author 14869
 */
@Slf4j
@Service
public class GaChaInfoServiceImpl implements GaChaInfoService {


    @Override
    public void getToken() {

    }

    @Override
    public String gaChaQueryByPage(int page, String token, Long qq) {
        return null;
    }

    @Override
    public void insertGaChaInfo(List<PoolsInfoDto> poolsInfoDtos, Long processId, Long qq) {

    }
}
