package com.arknights.bot.infra.constant;

/**
 * Created by wangzhen on 2023/1/20 11:16
 * @author 14869
 */
public interface Constance {

    String START_MARK = "#";
    String PRE_CHAR = "=";
    String AT_LOGO = "@W";

    int PAGE_SIZE = 10;
    int STR_LENGTH = 30;
    int SKILL_COUNTS = 10;
    String TYPE_F = "手动触发";
    String TYPE_S = "自动触发";

    String TYPE_SP_F = "自动回复";
    String TYPE_SP_S = "攻击回复";

    String TYPE_JUST_TEXT = "text";

    String TYPE_JUST_IMG = "img";

    String STATUS_SUCCESS = "SUCCESS";

    String STATUS_ERROR = "ERROR";

    String TOKEN_INSERT = "token录入";

    String TOKEN_DEMO = "token获取教程";

    String RED_ENVELOPE = "发红包";

    String GACHA_LOGO = "gacha";

    String SKILL_QUERY = "技能 ";

    String FK = "撅你";

    String SKILL_SPECIAL_F = "RankⅠ";
    String SKILL_SPECIAL_S = "RankⅡ";
    String SKILL_SPECIAL_T = "RankⅢ";

    /**
     * 技能相关
     */
    String SKILL_NAME_CH = "技能名=";
    String SKILL_TYPE_ONE = "技能类型1=";
    String SKILL_TYPE_TWO = "技能类型2=";
    String SKILL_DESC = "描述=";
    String SKILL_INIT = "初始=";
    String SKILL_CONSUME = "消耗=";
    String SKILL_SPAN = "持续=";
    String SKILL = "技能";
    String SKILL_SPECIALI = "技能专精";
    String REMARKS = "备注=";
    String REMARKS_EXTRA = "※";

    /**
     * 干员信息抓取相关
     */
    String OPERATOR_START_LOCAL = "特种";
    String OPERATOR_END_LOCAL = "取自";

    String ZERO = "0";
    int randomRevokeMaxNum = 6;

    /**
     * 临时代码
     */
    String KITTY_NAME = "chocolate";
}
