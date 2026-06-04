MERGE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `college`, `major`, `grade`, `role`, `status`) KEY(`id`) VALUES
(1, 'teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '教师', NULL, NULL, NULL, NULL, 'TEACHER', 1),
(2, 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '学生', NULL, '计算机科学与技术学院', '计算机科学与技术', '2024', 'STUDENT', 1);

MERGE INTO `sys_option` (`id`, `category`, `option_value`, `sort_order`) KEY(`id`) VALUES
(1, 'college', '计算机科学与技术学院', 1),
(2, 'college', '网络与信息安全学院', 2),
(3, 'college', '电子工程学院', 3),
(4, 'college', '通信工程学院', 4),
(5, 'college', '人工智能学院', 5),
(6, 'major', '计算机科学与技术', 1),
(7, 'major', '网络工程', 2),
(8, 'major', '信息安全', 3),
(9, 'major', '电子信息工程', 4),
(10, 'major', '通信工程', 5),
(11, 'major', '人工智能', 6),
(12, 'grade', '2022', 1),
(13, 'grade', '2023', 2),
(14, 'grade', '2024', 3),
(15, 'grade', '2025', 4);
