package com.arknights.bot.infra.mapper;

import com.arknights.bot.domain.entity.AccountInfo;

/**
 * Created by wangzhen on 2023/1/24 20:07
 * @author 14869
 */
public interface GroupChatMapper {
    /**
     * token信息插入更新
     * @param accountInfo
     * @return
     */
    Integer insertAccountInfo(AccountInfo accountInfo);

    /**
     * 账户信息查询
     * @param qq
     * @return
     */
    AccountInfo selectAccountInfo(Long qq);
}
