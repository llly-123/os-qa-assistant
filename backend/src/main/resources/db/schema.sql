CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(200) NOT NULL,
    `real_name` VARCHAR(50) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `college` VARCHAR(100) DEFAULT NULL,
    `major` VARCHAR(100) DEFAULT NULL,
    `grade` VARCHAR(20) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    `status` INT NOT NULL DEFAULT 1,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `class_id` BIGINT DEFAULT NULL,
    `title` VARCHAR(200) NOT NULL DEFAULT '新对话',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` CLOB NOT NULL,
    `citation` TEXT DEFAULT NULL,
    `source_type` VARCHAR(20) DEFAULT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

-- AI 提取的知识点关键词（JSON 数组字符串，如 ["晶体管","负反馈"]），用于热词统计
ALTER TABLE `chat_message` ADD COLUMN IF NOT EXISTS `keywords` VARCHAR(1000) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `knowledge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `kb_id` BIGINT DEFAULT NULL,
    `file_name` VARCHAR(200) NOT NULL,
    `file_path` VARCHAR(500) NOT NULL,
    `file_size` BIGINT DEFAULT 0,
    `chunk_count` INT DEFAULT 0,
    `status` INT NOT NULL DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `knowledge_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `knowledge_id` BIGINT NOT NULL,
    `kb_id` BIGINT DEFAULT NULL,
    `content` CLOB NOT NULL,
    `chunk_index` INT NOT NULL,
    `source_file` VARCHAR(200) DEFAULT NULL,
    `chapter_info` VARCHAR(200) DEFAULT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `sys_option` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `category` VARCHAR(50) NOT NULL,
    `option_value` VARCHAR(200) NOT NULL,
    `sort_order` INT DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `chapter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `video_set_id` BIGINT DEFAULT NULL,
    `title` VARCHAR(200) NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `section` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `chapter_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `video_url` VARCHAR(500) DEFAULT NULL,
    `video_size` BIGINT DEFAULT 0,
    `video_duration` INT DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `video_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `section_id` BIGINT NOT NULL,
    `play_time` DOUBLE DEFAULT 0,
    `completed` INT NOT NULL DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_user_section` UNIQUE (`user_id`, `section_id`)
);

CREATE TABLE IF NOT EXISTS `clazz` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `teacher_id` BIGINT NOT NULL,
    `video_set_id` BIGINT DEFAULT NULL,
    `kb_id` BIGINT DEFAULT NULL,
    `start_time` TIMESTAMP NOT NULL,
    `end_time` TIMESTAMP NOT NULL,
    `status` INT NOT NULL DEFAULT 1,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `class_student` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_class_student` UNIQUE (`class_id`, `student_id`)
);

-- ========== 知识库（教师可配置若干套）==========
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `teacher_id` BIGINT NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

-- ========== 视频集（教师可配置若干套）==========
CREATE TABLE IF NOT EXISTS `video_set` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `teacher_id` BIGINT NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

-- ========== 系统设置（站点名/课程名/学校名等，用于品牌化）==========
CREATE TABLE IF NOT EXISTS `system_setting` (
    `setting_key` VARCHAR(64) NOT NULL,
    `setting_value` VARCHAR(500) DEFAULT NULL,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`setting_key`)
);

-- ========== 已有库的列迁移（H2 2.x 支持 ADD COLUMN IF NOT EXISTS，幂等）==========
ALTER TABLE `chat_session` ADD COLUMN IF NOT EXISTS `class_id` BIGINT DEFAULT NULL;
ALTER TABLE `knowledge` ADD COLUMN IF NOT EXISTS `kb_id` BIGINT DEFAULT NULL;
ALTER TABLE `knowledge_chunk` ADD COLUMN IF NOT EXISTS `kb_id` BIGINT DEFAULT NULL;
ALTER TABLE `chapter` ADD COLUMN IF NOT EXISTS `video_set_id` BIGINT DEFAULT NULL;
ALTER TABLE `clazz` ADD COLUMN IF NOT EXISTS `video_set_id` BIGINT DEFAULT NULL;
ALTER TABLE `clazz` ADD COLUMN IF NOT EXISTS `kb_id` BIGINT DEFAULT NULL;

-- 初始化默认用户（仅当表为空时插入，不覆盖已有数据）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `college`, `major`, `grade`, `role`, `status`) SELECT 1, 'teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '教师', NULL, NULL, NULL, NULL, 'TEACHER', 1 WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `id` = 1);
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `college`, `major`, `grade`, `role`, `status`) SELECT 2, 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '学生', NULL, '计算机科学与技术学院', '计算机科学与技术', '2024', 'STUDENT', 1 WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `id` = 2);

INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 1, 'college', '计算机科学与技术学院', 1 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 1);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 2, 'college', '网络与信息安全学院', 2 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 2);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 3, 'college', '电子工程学院', 3 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 3);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 4, 'college', '通信工程学院', 4 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 4);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 5, 'college', '人工智能学院', 5 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 5);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 6, 'major', '计算机科学与技术', 1 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 6);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 7, 'major', '网络工程', 2 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 7);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 8, 'major', '信息安全', 3 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 8);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 9, 'major', '电子信息工程', 4 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 9);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 10, 'major', '通信工程', 5 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 10);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 11, 'major', '人工智能', 6 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 11);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 12, 'grade', '2022', 1 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 12);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 13, 'grade', '2023', 2 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 13);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 14, 'grade', '2024', 3 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 14);
INSERT INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) SELECT 15, 'grade', '2025', 4 WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `id` = 15);

-- 系统设置默认值（仅当对应键不存在时插入）
MERGE INTO `system_setting` (`setting_key`, `setting_value`) KEY(`setting_key`) VALUES ('site_name', '智能答疑助手');
MERGE INTO `system_setting` (`setting_key`, `setting_value`) KEY(`setting_key`) VALUES ('course_name', '本课程');
MERGE INTO `system_setting` (`setting_key`, `setting_value`) KEY(`setting_key`) VALUES ('school_name', '');

-- ========== 数据清洗：将历史孤儿知识文档/知识块关联到第一个知识库 ==========
-- knowledge 表中 kb_id 为 null 的记录关联到 id 最小的知识库（幂等，仅执行一次修复）
UPDATE `knowledge` SET `kb_id` = (SELECT MIN(id) FROM `knowledge_base` WHERE `deleted` = 0) WHERE `kb_id` IS NULL AND EXISTS (SELECT 1 FROM `knowledge_base` WHERE `deleted` = 0);
UPDATE `knowledge_chunk` SET `kb_id` = (SELECT MIN(id) FROM `knowledge_base` WHERE `deleted` = 0) WHERE `kb_id` IS NULL AND EXISTS (SELECT 1 FROM `knowledge_base` WHERE `deleted` = 0);
