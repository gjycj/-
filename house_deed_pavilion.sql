-- 创建数据库（多租户共享实例，通过tenant_id隔离）
CREATE DATABASE IF NOT EXISTS house_deed_pavilion;
USE house_deed_pavilion CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 1. 租户核心信息表（多租户基础）
CREATE TABLE `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户ID（唯一标识）',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码（分库/分表路由键，全局唯一）',
  `tenant_name` varchar(100) NOT NULL COMMENT '租户名称（如中介公司全称）',
  `contact_person` varchar(50) DEFAULT NULL COMMENT '租户联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系人电话',
  `domain` varchar(100) DEFAULT NULL COMMENT '租户独立域名（如租户自定义访问地址）',
  `config_json` json DEFAULT NULL COMMENT '租户个性化配置（如logo、流程开关等）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1-正常，0-禁用，2-过期）',
  `expire_time` datetime DEFAULT NULL COMMENT '租户过期时间（为空表示永久）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`) COMMENT '租户编码全局唯一',
  UNIQUE KEY `uk_tenant_domain` (`domain`) COMMENT '租户域名唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多租户核心信息表（租户隔离根表）';

-- 2. 租户配置表（补充租户级参数）
CREATE TABLE `tenant_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（关联tenant表，0为系统默认配置）',
  `config_key` varchar(50) NOT NULL COMMENT '配置项键（如COMMISSION_RATE、MAX_HOUSE_NUM）',
  `config_value` varchar(200) NOT NULL COMMENT '配置项值',
  `config_desc` varchar(200) DEFAULT NULL COMMENT '配置说明',
  `is_system` tinyint NOT NULL DEFAULT 0 COMMENT '是否系统内置（1-是，0-否）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_config_key` (`tenant_id`,`config_key`) COMMENT '租户内配置键唯一',
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户个性化配置表';

-- 3. 区域表（租户级行政区划，支持租户自定义区域）
CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '区域ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（0为系统默认区域，租户可扩展）',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级区域ID（0为顶级）',
  `region_name` varchar(100) NOT NULL COMMENT '区域名称（省/市/区/街道）',
  `region_level` tinyint NOT NULL COMMENT '层级（1-省，2-市，3-区，4-街道）',
  `region_code` varchar(20) DEFAULT NULL COMMENT '行政编码（如身份证前6位）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序（升序）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`) COMMENT '租户内按父级查询区域',
  KEY `idx_region_code` (`tenant_id`,`region_code`) COMMENT '租户内按行政编码查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域管理表（租户级数据隔离）';

-- 4. 门店表（租户下属门店）
CREATE TABLE `store` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '门店ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `store_code` varchar(50) NOT NULL COMMENT '门店编码（租户内唯一）',
  `store_name` varchar(100) NOT NULL COMMENT '门店名称',
  `region_id` bigint NOT NULL COMMENT '所属区域ID（关联region表，同租户）',
  `address` varchar(200) DEFAULT NULL COMMENT '详细地址',
  `manager_id` bigint DEFAULT NULL COMMENT '店长ID（关联agent表，同租户）',
  `phone` varchar(20) DEFAULT NULL COMMENT '门店电话',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1-营业，0-停业）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_store_code` (`tenant_id`,`store_code`) COMMENT '租户内门店编码唯一',
  KEY `idx_region` (`region_id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`) COMMENT '租户内按状态筛选门店'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店信息表（租户级数据）';

-- 5. 经纪人表（租户下属员工）
CREATE TABLE `agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '经纪人ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `store_id` bigint NOT NULL COMMENT '所属门店ID（关联store表，同租户）',
  `agent_code` varchar(50) NOT NULL COMMENT '工号（租户内唯一）',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `id_card` varchar(20) DEFAULT NULL COMMENT '身份证号',
  `position` varchar(50) DEFAULT NULL COMMENT '职位（经纪人/店长等）',
  `level` varchar(20) DEFAULT 'JUNIOR' COMMENT '级别（JUNIOR-初级，SENIOR-高级，STAR-明星）',
  `entry_time` date DEFAULT NULL COMMENT '入职时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1-在职，0-离职）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_agent_code` (`tenant_id`,`agent_code`) COMMENT '租户内工号唯一',
  KEY `idx_store` (`store_id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`) COMMENT '租户内按状态筛选经纪人'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经纪人信息表（租户级数据）';

-- 6. 客户表（租户的客户资源）
CREATE TABLE `customer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `id_card` varchar(20) DEFAULT NULL COMMENT '身份证号',
  `source` varchar(50) DEFAULT NULL COMMENT '客户来源（门店/线上/转介绍等）',
  `intended_region_id` bigint DEFAULT NULL COMMENT '意向区域ID（关联region表，同租户）',
  `intended_price_min` decimal(12,2) DEFAULT NULL COMMENT '意向价格下限（万元）',
  `intended_price_max` decimal(12,2) DEFAULT NULL COMMENT '意向价格上限（万元）',
  `intended_house_type` varchar(50) DEFAULT NULL COMMENT '意向户型（如两居室）',
  `customer_type` varchar(20) DEFAULT 'ORDINARY' COMMENT '客户类型（ORDINARY-普通，VIP-会员，INVEST-投资客）',
  `potential_level` tinyint DEFAULT 2 COMMENT '潜力等级（1-低，2-中，3-高）',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE-活跃，DEALED-已成交，DORMANT-休眠）',
  `create_agent_id` bigint DEFAULT NULL COMMENT '创建人（经纪人ID，同租户）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_phone` (`tenant_id`,`phone`) COMMENT '租户内按电话查询客户',
  KEY `idx_intended_region` (`intended_region_id`),
  KEY `idx_customer_type_potential` (`customer_type`,`potential_level`) COMMENT '租户内筛选重点客户'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户信息表（租户级数据隔离）';

-- 7. 客户跟进日志表（租户内客户跟进记录）
CREATE TABLE `customer_follow_up` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `customer_id` bigint NOT NULL COMMENT '客户ID（关联customer表，同租户）',
  `agent_id` bigint NOT NULL COMMENT '跟进经纪人ID（关联agent表，同租户）',
  `follow_time` datetime NOT NULL COMMENT '跟进时间',
  `content` text COMMENT '跟进内容（如需求变化、看过的房源反馈等）',
  `demand_change` text COMMENT '需求调整记录（如从两居改三居）',
  `next_follow_plan` varchar(200) DEFAULT NULL COMMENT '下次跟进计划',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_customer_agent` (`customer_id`,`agent_id`),
  KEY `idx_tenant_follow_time` (`tenant_id`,`follow_time`) COMMENT '租户内按时间查询跟进记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户跟进记录表（租户级数据）';

-- 8. 客户历史成交关联表（租户内客户成交记录）
CREATE TABLE `customer_history_deal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `customer_id` bigint NOT NULL COMMENT '客户ID（关联customer表，同租户）',
  `contract_id` bigint NOT NULL COMMENT '历史成交合同ID（关联contract表，同租户）',
  `deal_time` date NOT NULL COMMENT '成交日期',
  `house_info` varchar(200) NOT NULL COMMENT '成交房源简要信息（如XX小区3室2厅）',
  `deal_type` varchar(20) NOT NULL COMMENT '成交类型（SALE-买卖，RENT-租赁）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`),
  UNIQUE KEY `uk_customer_contract` (`customer_id`,`contract_id`) COMMENT '客户-合同关联唯一',
  KEY `idx_tenant_deal_time` (`tenant_id`,`deal_time`) COMMENT '租户内按时间统计成交'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户历史成交记录表（租户级数据）';

-- 9. 房东表（租户管理的房东资源）
CREATE TABLE `landlord` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房东ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `id_card` varchar(20) DEFAULT NULL COMMENT '身份证号',
  `address` varchar(200) DEFAULT NULL COMMENT '联系地址',
  `create_agent_id` bigint DEFAULT NULL COMMENT '创建人（经纪人ID，同租户）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_phone` (`tenant_id`,`phone`) COMMENT '租户内按电话查询房东',
  KEY `idx_create_agent` (`create_agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房东信息表（租户级数据隔离）';

-- 10. 房东委托表（租户内房东委托关系）
CREATE TABLE `landlord_entrust` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '委托ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `landlord_id` bigint NOT NULL COMMENT '房东ID（关联landlord表，同租户）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `entrust_type` varchar(20) NOT NULL COMMENT '委托类型（EXCLUSIVE-独家，NON_EXCLUSIVE-非独家）',
  `entrust_start_time` date NOT NULL COMMENT '委托开始时间',
  `entrust_end_time` date NOT NULL COMMENT '委托结束时间',
  `renew_remind` tinyint NOT NULL DEFAULT 1 COMMENT '是否到期提醒（1-是，0-否）',
  `remark` text COMMENT '委托备注（如特殊要求）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1-有效，0-过期/取消）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_landlord_house` (`landlord_id`,`house_id`) COMMENT '房东-房源委托关系唯一',
  KEY `idx_entrust_end` (`entrust_end_time`),
  KEY `idx_tenant_status` (`tenant_id`,`status`) COMMENT '租户内筛选有效委托'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房东委托信息表（租户级数据）';

-- 11. 楼盘表（租户管理的楼盘资源）
CREATE TABLE `property` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '楼盘ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `property_name` varchar(100) NOT NULL COMMENT '楼盘名称',
  `region_id` bigint NOT NULL COMMENT '所属区域ID（关联region表，同租户）',
  `address` varchar(200) DEFAULT NULL COMMENT '详细地址',
  `developer` varchar(100) DEFAULT NULL COMMENT '开发商',
  `green_rate` decimal(5,2) DEFAULT NULL COMMENT '绿化率（%）',
  `completion_year` int DEFAULT NULL COMMENT '建成年份',
  `property_management` varchar(100) DEFAULT NULL COMMENT '物业公司',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_region` (`region_id`),
  KEY `idx_tenant_name` (`tenant_id`,`property_name`) COMMENT '租户内按名称查询楼盘'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼盘信息表（租户级数据隔离）';

-- 12. 楼栋表（租户管理的楼栋资源）
CREATE TABLE `building` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '楼栋ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `property_id` bigint NOT NULL COMMENT '所属楼盘ID（关联property表，同租户）',
  `building_no` varchar(50) NOT NULL COMMENT '楼栋号（如1号楼）',
  `unit_count` int DEFAULT NULL COMMENT '单元数',
  `total_floor` int DEFAULT NULL COMMENT '总层数',
  `building_type` varchar(50) DEFAULT NULL COMMENT '建筑类型（板楼/塔楼等）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_property_building_no` (`property_id`,`building_no`) COMMENT '楼盘内楼栋号唯一',
  KEY `idx_tenant_property` (`tenant_id`,`property_id`) COMMENT '租户内按楼盘查询楼栋'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼栋信息表（租户级数据）';

-- 13. 房源表（租户核心资源）
CREATE TABLE `house` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房源ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `building_id` bigint NOT NULL COMMENT '所属楼栋ID（关联building表，同租户）',
  `house_no` varchar(50) NOT NULL COMMENT '房号（如1单元301）',
  `house_type` varchar(50) NOT NULL COMMENT '户型（如3室2厅）',
  `area` decimal(8,2) NOT NULL COMMENT '建筑面积（㎡）',
  `inside_area` decimal(8,2) DEFAULT NULL COMMENT '套内面积（㎡）',
  `floor` int NOT NULL COMMENT '所在楼层',
  `total_floor` int NOT NULL COMMENT '总楼层',
  `orientation` varchar(20) DEFAULT NULL COMMENT '朝向（南北通透等）',
  `decoration` varchar(20) DEFAULT NULL COMMENT '装修情况（毛坯/简装/精装）',
  `property_right` varchar(20) NOT NULL COMMENT '产权性质（商品房/经济适用房等）',
  `property_right_cert_no` varchar(50) DEFAULT NULL COMMENT '产权证号',
  `property_right_years` int DEFAULT NULL COMMENT '产权年限',
  `mortgage_status` varchar(20) DEFAULT NULL COMMENT '抵押状态（NONE-无抵押，MORTGAGED-已抵押）',
  `mortgage_details` text COMMENT '抵押详情（如抵押银行、金额）',
  `price` decimal(12,2) NOT NULL COMMENT '挂牌价（万元）',
  `transaction_type` varchar(20) NOT NULL COMMENT '交易类型（SALE-出售，RENT-出租，BOTH-可售可租）',
  `status` varchar(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '状态（ON_SALE-在售，RESERVED-已预订，SOLD-已售，OFF_SHELF-下架）',
  `description` text COMMENT '房源描述',
  `create_agent_id` bigint NOT NULL COMMENT '录入经纪人ID（同租户）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_house_no` (`building_id`,`house_no`) COMMENT '楼栋内房号唯一',
  KEY `idx_status_transaction` (`status`,`transaction_type`),
  KEY `idx_price` (`price`),
  KEY `idx_tenant_create_agent` (`tenant_id`,`create_agent_id`) COMMENT '租户内按经纪人查询房源'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源信息表（租户核心数据）';

-- 14. 房源价格变动日志表（租户内价格追踪）
CREATE TABLE `house_price_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `price_before` decimal(12,2) NOT NULL COMMENT '调整前价格',
  `price_after` decimal(12,2) NOT NULL COMMENT '调整后价格',
  `change_reason` varchar(200) NOT NULL COMMENT '调价原因（房东要求/市场行情/促销等）',
  `operator_id` bigint NOT NULL COMMENT '操作人ID（经纪人，同租户）',
  `operator_name` varchar(50) NOT NULL COMMENT '操作人姓名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调价时间',
  PRIMARY KEY (`id`),
  KEY `idx_house` (`house_id`),
  KEY `idx_tenant_create_time` (`tenant_id`,`create_time`) COMMENT '租户内按时间查询调价记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源价格变动记录表（租户级数据）';

-- 15. 房源-房东关联表（租户内权属关系）
CREATE TABLE `house_landlord` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `landlord_id` bigint NOT NULL COMMENT '房东ID（关联landlord表，同租户）',
  `ownership` varchar(50) DEFAULT NULL COMMENT '所有权占比（如100%）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_house_landlord` (`house_id`,`landlord_id`) COMMENT '房源-房东关联唯一',
  KEY `idx_tenant_house` (`tenant_id`,`house_id`) COMMENT '租户内查询房源所属房东'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源与房东关联表（租户级数据）';

-- 16. 标签表（租户自定义标签）
CREATE TABLE `tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `tag_name` varchar(50) NOT NULL COMMENT '标签名称（如学区房、近地铁）',
  `tag_type` varchar(50) DEFAULT NULL COMMENT '标签类型（房源标签/客户标签）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_tag` (`tenant_id`,`tag_name`,`tag_type`) COMMENT '租户内标签名称+类型唯一',
  KEY `idx_tenant_type` (`tenant_id`,`tag_type`) COMMENT '租户内按类型查询标签'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表（租户级数据隔离）';

-- 17. 房源-标签关联表（租户内房源标签关系）
CREATE TABLE `house_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `tag_id` bigint NOT NULL COMMENT '标签ID（关联tag表，同租户）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_house_tag` (`house_id`,`tag_id`) COMMENT '房源-标签关联唯一',
  KEY `idx_tenant_tag` (`tenant_id`,`tag_id`) COMMENT '租户内按标签查询房源'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源与标签关联表（租户级数据）';

-- 18. 房源图片表（租户内房源图片）
CREATE TABLE `house_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `image_type` varchar(20) DEFAULT NULL COMMENT '图片类型（COVER-封面，LIVING_ROOM-客厅等）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序（数字越小越靠前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_house` (`house_id`),
  KEY `idx_tenant_image_type` (`tenant_id`,`image_type`) COMMENT '租户内按图片类型筛选'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源图片表（租户级数据）';

-- 19. 带看记录表（租户内带看记录）
CREATE TABLE `visit_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '带看ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `customer_id` bigint NOT NULL COMMENT '客户ID（关联customer表，同租户）',
  `agent_id` bigint NOT NULL COMMENT '带看经纪人ID（关联agent表，同租户）',
  `visit_time` datetime NOT NULL COMMENT '带看时间',
  `visit_type` varchar(20) DEFAULT NULL COMMENT '带看方式（线下/VR）',
  `customer_feedback` text COMMENT '客户反馈（如价格太高、户型满意）',
  `intention_level` tinyint DEFAULT NULL COMMENT '意向程度（1-低，2-中，3-高）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_customer_house` (`customer_id`,`house_id`),
  KEY `idx_tenant_visit_time` (`tenant_id`,`visit_time`) COMMENT '租户内按时间查询带看记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='带看记录表（租户级数据）';

-- 20. 合同表（租户内交易合同）
CREATE TABLE `contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `contract_no` varchar(50) NOT NULL COMMENT '合同编号（租户内唯一）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `customer_id` bigint NOT NULL COMMENT '客户ID（关联customer表，同租户）',
  `landlord_id` bigint NOT NULL COMMENT '房东ID（关联landlord表，同租户）',
  `agent_id` bigint NOT NULL COMMENT '签约经纪人ID（关联agent表，同租户）',
  `contract_type` varchar(20) NOT NULL COMMENT '合同类型（SALE-买卖合同，RENT-租赁合同）',
  `amount` decimal(12,2) NOT NULL COMMENT '交易总金额（万元/租金总额）',
  `deposit` decimal(12,2) DEFAULT NULL COMMENT '定金/押金（万元）',
  `payment_method` varchar(50) DEFAULT NULL COMMENT '付款方式（全款/按揭/月付等）',
  `sign_time` datetime NOT NULL COMMENT '签约时间',
  `start_date` date DEFAULT NULL COMMENT '生效日期（租赁适用）',
  `end_date` date DEFAULT NULL COMMENT '到期日期（租赁适用）',
  `status` varchar(20) NOT NULL COMMENT '状态（SIGNED-已签约，EXECUTING-执行中，COMPLETED-已完成，TERMINATED-已终止）',
  `remark` text COMMENT '其他约定',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_contract_no` (`tenant_id`,`contract_no`) COMMENT '租户内合同编号唯一',
  KEY `idx_house` (`house_id`),
  KEY `idx_tenant_sign_time` (`tenant_id`,`sign_time`) COMMENT '租户内按签约时间查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易合同表（租户核心业务数据）';

-- 21. 电子签约信息表（租户内电子签记录）
CREATE TABLE `electronic_sign` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '电子签ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `contract_id` bigint NOT NULL COMMENT '合同ID（关联contract表，同租户）',
  `sign_platform` varchar(50) DEFAULT NULL COMMENT '电子签平台（如e签宝/法大大）',
  `sign_url` varchar(500) DEFAULT NULL COMMENT '签约链接',
  `sign_status` varchar(20) NOT NULL COMMENT '签约状态（PENDING-待签，SIGNED-已签，REJECTED-拒签，EXPIRED-过期）',
  `customer_sign_time` datetime DEFAULT NULL COMMENT '客户签约时间',
  `landlord_sign_time` datetime DEFAULT NULL COMMENT '房东签约时间',
  `sign_hash` varchar(255) DEFAULT NULL COMMENT '电子签名哈希值（防篡改）',
  `contract_pdf_url` varchar(500) DEFAULT NULL COMMENT '电子合同PDF地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract` (`contract_id`),
  KEY `idx_tenant_sign_status` (`tenant_id`,`sign_status`) COMMENT '租户内按签约状态查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子签约信息表（租户级数据）';

-- 22. 合同附件表（租户内合同附件）
CREATE TABLE `contract_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `contract_id` bigint NOT NULL COMMENT '合同ID（关联contract表，同租户）',
  `attachment_type` varchar(50) NOT NULL COMMENT '附件类型（ID_CARD-身份证，PROPERTY_CERT-房产证等）',
  `attachment_url` varchar(500) NOT NULL COMMENT '附件URL',
  `file_name` varchar(100) DEFAULT NULL COMMENT '文件名称',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `uploader_id` bigint NOT NULL COMMENT '上传人ID（经纪人，同租户）',
  PRIMARY KEY (`id`),
  KEY `idx_contract_type` (`contract_id`,`attachment_type`) COMMENT '按合同+类型查询',
  KEY `idx_tenant_upload_time` (`tenant_id`,`upload_time`) COMMENT '租户内按上传时间查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同附件表（租户级数据）';

-- 23. 租赁合同附加条款表（租户内租赁补充条款）
CREATE TABLE `contract_lease_terms` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '条款ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `contract_id` bigint NOT NULL COMMENT '合同ID（关联contract表，同租户，仅租赁）',
  `allow_pet` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许养宠物（1-是，0-否）',
  `allow_sublet` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许转租（1-是，0-否）',
  `fee_bear` varchar(100) DEFAULT NULL COMMENT '费用承担（如物业费房东承担，水电费租户承担）',
  `furniture_maintenance` text COMMENT '家具维修约定（如自然损坏房东负责）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract` (`contract_id`),
  KEY `idx_tenant_contract` (`tenant_id`,`contract_id`) COMMENT '租户内查询租赁条款'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁合同附加条款表（租户级数据）';

-- 24. 房屋交接表（租户内租赁交接记录）
CREATE TABLE `house_handover` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交接ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `contract_id` bigint NOT NULL COMMENT '合同ID（关联contract表，同租户，仅租赁）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `handover_type` varchar(20) NOT NULL COMMENT '交接类型（CHECK_IN-入住，CHECK_OUT-退租）',
  `handover_time` datetime NOT NULL COMMENT '交接时间',
  `appliances_list` json DEFAULT NULL COMMENT '家具家电清单（如{"冰箱":"海尔","空调":2台}）',
  `water_meter` decimal(10,2) DEFAULT NULL COMMENT '水表底数（吨）',
  `electricity_meter` decimal(10,2) DEFAULT NULL COMMENT '电表底数（度）',
  `gas_meter` decimal(10,2) DEFAULT NULL COMMENT '燃气表底数（立方米）',
  `damage_records` text COMMENT '房屋损坏记录（如墙面划痕）',
  `handover_person` varchar(50) NOT NULL COMMENT '交接人（房东或其代理人）',
  `receiver` varchar(50) NOT NULL COMMENT '接收人（租户）',
  `sign_image_url` varchar(500) DEFAULT NULL COMMENT '交接确认签字图片URL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`),
  KEY `idx_tenant_handover_time` (`tenant_id`,`handover_time`) COMMENT '租户内按交接时间查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋交接记录表（租户级数据）';

-- 25. 交易费用明细表（租户内交易费用）
CREATE TABLE `transaction_fee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '费用ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `contract_id` bigint NOT NULL COMMENT '合同ID（关联contract表，同租户）',
  `fee_type` varchar(50) NOT NULL COMMENT '费用类型（AGENCY_FEE-中介费，TAX-税费等）',
  `amount` decimal(10,2) NOT NULL COMMENT '费用金额（元）',
  `payer` varchar(20) NOT NULL COMMENT '支付方（CUSTOMER-客户，LANDLORD-房东）',
  `payment_status` varchar(20) NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态（UNPAID-未付，PAID-已付）',
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注（如发票号）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`),
  KEY `idx_tenant_payment_status` (`tenant_id`,`payment_status`) COMMENT '租户内按支付状态查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易费用明细表（租户级数据）';

-- 26. 贷款信息表（租户内按揭流程）
CREATE TABLE `loan_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '贷款ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `contract_id` bigint NOT NULL COMMENT '合同ID（关联contract表，同租户，仅买卖）',
  `bank_name` varchar(100) NOT NULL COMMENT '贷款银行',
  `loan_amount` decimal(12,2) NOT NULL COMMENT '贷款金额（万元）',
  `loan_term` int NOT NULL COMMENT '贷款期限（月）',
  `interest_rate` decimal(5,4) NOT NULL COMMENT '贷款利率（如0.049表示4.9%）',
  `loan_type` varchar(50) DEFAULT NULL COMMENT '贷款类型（COMMERCIAL-商业贷款，FUND-公积金贷款）',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `approve_time` datetime DEFAULT NULL COMMENT '审批通过时间',
  `loan_status` varchar(20) NOT NULL DEFAULT 'APPLYING' COMMENT '贷款状态（APPLYING-申请中，APPROVED-已审批，REJECTED-被拒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`),
  KEY `idx_tenant_loan_status` (`tenant_id`,`loan_status`) COMMENT '租户内按贷款状态查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款信息表（租户级数据）';

-- 27. 贷款材料提交记录表（租户内贷款材料）
CREATE TABLE `loan_material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '材料ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `loan_id` bigint NOT NULL COMMENT '贷款ID（关联loan_info表，同租户）',
  `material_type` varchar(50) NOT NULL COMMENT '材料类型（INCOME_PROOF-收入证明等）',
  `status` varchar(20) NOT NULL DEFAULT 'UNSUBMITTED' COMMENT '状态（UNSUBMITTED-未提交等）',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `reject_reason` varchar(200) DEFAULT NULL COMMENT '不合格原因',
  `material_url` varchar(500) DEFAULT NULL COMMENT '材料扫描件URL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_loan_material_type` (`loan_id`,`material_type`) COMMENT '贷款-材料类型唯一',
  KEY `idx_tenant_status` (`tenant_id`,`status`) COMMENT '租户内按材料状态查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款材料提交记录表（租户级数据）';

-- 28. 维修工单表（租户内房源维修）
CREATE TABLE `maintenance_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `order_no` varchar(50) NOT NULL COMMENT '工单编号（租户内唯一）',
  `reporter_type` varchar(20) NOT NULL COMMENT '报修人类型（TENANT-租户等）',
  `reporter_id` bigint NOT NULL COMMENT '报修人ID（关联对应表，同租户）',
  `reporter_phone` varchar(20) NOT NULL COMMENT '报修人电话',
  `maintenance_type` varchar(50) NOT NULL COMMENT '维修类型（WATER-水电，APPLIANCE-家电等）',
  `description` text NOT NULL COMMENT '故障描述（如空调不制冷）',
  `urgency_level` tinyint NOT NULL DEFAULT 2 COMMENT '紧急程度（1-低，2-中，3-高）',
  `repairman_id` bigint DEFAULT NULL COMMENT '维修师傅ID（可关联外部表）',
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态（SUBMITTED-已提交等）',
  `appointment_time` datetime DEFAULT NULL COMMENT '预约维修时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cost_amount` decimal(8,2) DEFAULT NULL COMMENT '维修费用（元）',
  `cost_bearer` varchar(20) DEFAULT NULL COMMENT '费用承担方（LANDLORD-房东等）',
  `remark` text COMMENT '维修结果备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_order_no` (`tenant_id`,`order_no`) COMMENT '租户内工单编号唯一',
  KEY `idx_house_status` (`house_id`,`status`),
  KEY `idx_tenant_urgency` (`tenant_id`,`urgency_level`) COMMENT '租户内按紧急程度查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源维修工单表（租户级数据）';

-- 29. 房源维护计划表（租户内定期维护）
CREATE TABLE `house_maintain_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `maintain_type` varchar(50) NOT NULL COMMENT '维护类型（CLEAN-保洁等）',
  `cycle` varchar(20) DEFAULT NULL COMMENT '周期（ONCE-一次性，WEEKLY-每周等）',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期（一次性维护可为空）',
  `executor_id` bigint DEFAULT NULL COMMENT '执行人ID（经纪人/第三方，同租户）',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE-生效中等）',
  `remark` varchar(200) DEFAULT NULL COMMENT '维护要求（如每周六保洁）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_house` (`house_id`),
  KEY `idx_tenant_executor` (`tenant_id`,`executor_id`) COMMENT '租户内按执行人查询计划'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源维护计划表（租户级数据）';

-- 30. 经纪人业绩表（租户内业绩统计）
CREATE TABLE `agent_performance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '业绩ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `agent_id` bigint NOT NULL COMMENT '经纪人ID（关联agent表，同租户）',
  `contract_id` bigint NOT NULL COMMENT '成交合同ID（关联contract表，同租户）',
  `performance_month` varchar(6) NOT NULL COMMENT '业绩月份（如202310）',
  `deal_amount` decimal(12,2) NOT NULL COMMENT '成交金额（万元）',
  `commission_amount` decimal(10,2) NOT NULL COMMENT '佣金金额（元）',
  `performance_status` varchar(20) NOT NULL DEFAULT 'UNSETTLED' COMMENT '业绩状态（UNSETTLED-未结算等）',
  `settle_time` datetime DEFAULT NULL COMMENT '结算时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_contract` (`agent_id`,`contract_id`) COMMENT '经纪人-合同业绩唯一',
  KEY `idx_tenant_performance_month` (`tenant_id`,`performance_month`) COMMENT '租户内按月份统计业绩',
  KEY `idx_status` (`performance_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经纪人业绩记录表（租户级数据）';

-- 31. 佣金规则表（租户内佣金计算规则）
CREATE TABLE `commission_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称（如“独家房源提成规则”）',
  `applicable_type` varchar(50) NOT NULL COMMENT '适用类型（EXCLUSIVE_HOUSE-独家房源等）',
  `condition` varchar(200) NOT NULL COMMENT '规则条件（如“agent_level = ''STAR''”）',
  `commission_rate` decimal(5,4) NOT NULL COMMENT '佣金比例（如0.15代表15%）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1-生效，0-失效）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_applicable_type` (`tenant_id`,`applicable_type`) COMMENT '租户内按适用类型查询规则',
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金计算规则表（租户级数据）';

-- 32. 投诉与纠纷表（租户内纠纷处理）
CREATE TABLE `complaint_dispute` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '纠纷ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `dispute_no` varchar(50) NOT NULL COMMENT '纠纷编号（租户内唯一）',
  `related_contract_id` bigint DEFAULT NULL COMMENT '关联合同ID（可空，同租户）',
  `complainant_type` varchar(20) NOT NULL COMMENT '投诉人类型（CUSTOMER-客户等）',
  `complainant_id` bigint DEFAULT NULL COMMENT '投诉人ID（关联对应表，同租户）',
  `complainant_phone` varchar(20) NOT NULL COMMENT '投诉人电话',
  `dispute_type` varchar(50) NOT NULL COMMENT '纠纷类型（SERVICE-服务投诉等）',
  `description` text NOT NULL COMMENT '纠纷描述',
  `status` varchar(20) NOT NULL DEFAULT 'ACCEPTED' COMMENT '状态（ACCEPTED-已受理等）',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID（管理员/店长，同租户）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_dispute_no` (`tenant_id`,`dispute_no`) COMMENT '租户内纠纷编号唯一',
  KEY `idx_related_contract` (`related_contract_id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`) COMMENT '租户内按状态查询纠纷'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉与纠纷记录表（租户级数据）';

-- 33. 纠纷处理日志表（租户内纠纷处理过程）
CREATE TABLE `dispute_handle_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `dispute_id` bigint NOT NULL COMMENT '纠纷ID（关联complaint_dispute表，同租户）',
  `handle_time` datetime NOT NULL COMMENT '处理时间',
  `handler_id` bigint NOT NULL COMMENT '处理人ID（同租户）',
  `handler_name` varchar(50) NOT NULL COMMENT '处理人姓名',
  `handle_content` text NOT NULL COMMENT '处理内容（如沟通记录、解决方案）',
  `status_before` varchar(20) DEFAULT NULL COMMENT '处理前状态',
  `status_after` varchar(20) DEFAULT NULL COMMENT '处理后状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dispute` (`dispute_id`),
  KEY `idx_tenant_handle_time` (`tenant_id`,`handle_time`) COMMENT '租户内按处理时间查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='纠纷处理日志表（租户级数据）';

-- 34. 房源状态变更日志表（租户内房源状态追踪）
CREATE TABLE `house_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `house_id` bigint NOT NULL COMMENT '房源ID（关联house表，同租户）',
  `status_before` varchar(20) NOT NULL COMMENT '变更前状态',
  `status_after` varchar(20) NOT NULL COMMENT '变更后状态',
  `change_reason` text NOT NULL COMMENT '变更原因（如客户支付诚意金）',
  `operator_id` bigint NOT NULL COMMENT '操作人ID（经纪人ID，同租户）',
  `operator_name` varchar(50) NOT NULL COMMENT '操作人姓名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_house` (`house_id`),
  KEY `idx_tenant_create_time` (`tenant_id`,`create_time`) COMMENT '租户内按时间查询状态变更'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源状态变更日志表（租户级数据）';

-- 35. 系统操作日志表（租户内系统操作审计）
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户，0为系统操作）',
  `module` varchar(50) NOT NULL COMMENT '操作模块（房源管理/客户管理等）',
  `operation_type` varchar(20) NOT NULL COMMENT '操作类型（ADD-新增，UPDATE-修改等）',
  `operation_content` text COMMENT '操作内容（如修改房源价格从100万到105万）',
  `operator_id` bigint NOT NULL COMMENT '操作人ID（经纪人/管理员，同租户）',
  `operator_name` varchar(50) NOT NULL COMMENT '操作人姓名',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '操作IP地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_module_time` (`module`,`create_time`),
  KEY `idx_tenant_operator` (`tenant_id`,`operator_id`) COMMENT '租户内按操作人查询日志'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表（租户级审计）';

-- 36. 租户数据备份表（租户数据删除存档）
-- 经纪人备份
CREATE TABLE `agent_backup` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '备份ID',
  `original_id` bigint NOT NULL COMMENT '原经纪人ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `store_id` bigint NOT NULL,
  `agent_code` varchar(50) NOT NULL,
  `name` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `id_card` varchar(20) DEFAULT NULL,
  `position` varchar(50) DEFAULT NULL,
  `level` varchar(20) DEFAULT NULL,
  `entry_time` date DEFAULT NULL,
  `status` tinyint NOT NULL,
  `delete_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `delete_operator` varchar(50) NOT NULL COMMENT '删除人',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_original_id` (`tenant_id`,`original_id`) COMMENT '租户内按原ID查询备份'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经纪人删除备份表（租户级存档）';

-- 客户备份
CREATE TABLE `customer_backup` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '备份ID',
  `original_id` bigint NOT NULL COMMENT '原客户ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `name` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `id_card` varchar(20) DEFAULT NULL,
  `source` varchar(50) DEFAULT NULL,
  `intended_region_id` bigint DEFAULT NULL,
  `intended_price_min` decimal(12,2) DEFAULT NULL,
  `intended_price_max` decimal(12,2) DEFAULT NULL,
  `intended_house_type` varchar(50) DEFAULT NULL,
  `customer_type` varchar(20) DEFAULT NULL,
  `potential_level` tinyint DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `create_agent_id` bigint DEFAULT NULL,
  `delete_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `delete_operator` varchar(50) NOT NULL COMMENT '删除人',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_original_id` (`tenant_id`,`original_id`) COMMENT '租户内按原ID查询备份'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户删除备份表（租户级存档）';

-- 房源备份
CREATE TABLE `house_backup` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '备份ID',
  `original_id` bigint NOT NULL COMMENT '原房源ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID（归属租户）',
  `building_id` bigint NOT NULL,
  `house_no` varchar(50) NOT NULL,
  `house_type` varchar(50) NOT NULL,
  `area` decimal(8,2) NOT NULL,
  `inside_area` decimal(8,2) DEFAULT NULL,
  `floor` int NOT NULL,
  `total_floor` int NOT NULL,
  `orientation` varchar(20) DEFAULT NULL,
  `decoration` varchar(20) DEFAULT NULL,
  `property_right` varchar(20) NOT NULL,
  `property_right_cert_no` varchar(50) DEFAULT NULL,
  `property_right_years` int DEFAULT NULL,
  `mortgage_status` varchar(20) DEFAULT NULL,
  `mortgage_details` text,
  `price` decimal(12,2) NOT NULL,
  `transaction_type` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `description` text COMMENT '房源描述',
  `create_agent_id` bigint NOT NULL,
  `delete_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `delete_operator` varchar(50) NOT NULL COMMENT '删除人',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_original_id` (`tenant_id`,`original_id`) COMMENT '租户内按原ID查询备份'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源删除备份表（租户级存档）';