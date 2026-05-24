MERGE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `role`, `status`) KEY(`id`) VALUES
(1, 'teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '教师', 'teacher@xidian.edu.cn', 'TEACHER', 1),
(2, 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '学生', 'student@xidian.edu.cn', 'STUDENT', 1);
