-- 员工全周期治理系统：初始化库与业务表
CREATE DATABASE IF NOT EXISTS `workforce`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `workforce`;

SOURCE /init/workforce/01_init.sql;
SOURCE /init/workforce/02_lifecycle.sql;
SOURCE /init/workforce/03_lifecycle_action.sql;
