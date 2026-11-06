-- 服务交付系统：初始化库与业务表
CREATE DATABASE IF NOT EXISTS `servicedesk`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `servicedesk`;

SOURCE /init/servicedesk/01_init.sql;
SOURCE /init/servicedesk/02_it_ticket.sql;
SOURCE /init/servicedesk/03_ai_intake.sql;
SOURCE /init/servicedesk/05_quartz.sql;
SOURCE /init/servicedesk/04_demo_data.sql;
