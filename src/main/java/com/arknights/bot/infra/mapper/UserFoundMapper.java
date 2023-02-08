package com.arknights.bot.infra.mapper;


import java.util.List;

/**
 * @author wangzy
 * @Date 2020/12/7 13:50
 **/
public interface UserFoundMapper {

    /**
     * 对指定群组进行定时消息发送
     * @return
     */
    List<Long> selectAllGroups();


}
