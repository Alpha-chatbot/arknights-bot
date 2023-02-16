-- 创建数据库
CREATE DATABASE arknights;
-- 建表
-- qq 映射表
CREATE TABLE `account_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `token` varchar(250) DEFAULT NULL,
  `qq` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `account_info_uk` (`qq`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- 群聊信息表
CREATE TABLE `a_group_admin` (
  `group_id` int NOT NULL,
  `found` int DEFAULT '20',
  `picture` int DEFAULT '0',
  PRIMARY KEY (`group_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC

-- 卡池寻访表
CREATE TABLE `gacha_info` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `operators_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '干员名称',
                              `ts` bigint NOT NULL COMMENT '唯一性时间戳',
                              `pool` varchar(80) DEFAULT NULL COMMENT '卡池名称',
                              `rarity` tinyint DEFAULT NULL,
                              `is_new` tinyint(1) DEFAULT '0' COMMENT '是否新获取',
                              `process_id` bigint DEFAULT NULL COMMENT '批次号',
                              `qq` bigint DEFAULT NULL,
                              `gacha_time` varchar(80) DEFAULT NULL COMMENT '寻访时间',
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3689 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci


-- 干员信息表
CREATE TABLE `operator_base_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `key_code` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `en_name` varchar(60) DEFAULT NULL COMMENT '干员英文名',
  `zh_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '干员中文名',
  `item_usage` varchar(60) DEFAULT NULL COMMENT '招聘合同',
  `mapping_code` varchar(60) DEFAULT NULL COMMENT '关联code',
  PRIMARY KEY (`id`),
  UNIQUE KEY `operator_uk` (`key_code`)
) ENGINE=InnoDB AUTO_INCREMENT=268 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci


-- 干员技能信息表
CREATE TABLE `skill_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `skill_code` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '技能编码',
  `skill_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '技能名称',
  `power_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '技力类型',
  `trigger_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '触发类型',
  `skill_level` bigint DEFAULT NULL COMMENT '等级(专精为8-10)',
  `description` varchar(600) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '技能描述',
  `initial_value` bigint DEFAULT '0' COMMENT '初始技力',
  `consume_value` bigint DEFAULT NULL COMMENT '消耗技力',
  `span` bigint DEFAULT NULL COMMENT '技能持续时间',
  `remarks` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `open_level` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '技能解锁条件',
  `skill_order` int(10) unsigned zerofill DEFAULT NULL COMMENT '技能序号(最大应为3)',
  `attribute1` varchar(255) DEFAULT NULL COMMENT '附加字段1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `skill_info_uk1` (`skill_code`,`skill_order`,`skill_level`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6315 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci


-- 技能与干员信息映射表
CREATE TABLE `skill_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_code` varchar(90) DEFAULT NULL COMMENT '干员代码',
  `skill_code` varchar(90) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '技能编号(对应json的id)',
  `open_level` varchar(20) DEFAULT NULL COMMENT '开启条件',
  `skill_order` int DEFAULT NULL COMMENT '技能顺序',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=565 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

