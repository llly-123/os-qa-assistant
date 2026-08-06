package com.xidian.osqa.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("study_time")
public class StudyTime {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("class_id")
    private Long classId;
    @TableField("study_date")
    private LocalDate studyDate;
    @TableField("total_seconds")
    private Integer totalSeconds;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public LocalDate getStudyDate() { return studyDate; }
    public void setStudyDate(LocalDate studyDate) { this.studyDate = studyDate; }
    public Integer getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(Integer totalSeconds) { this.totalSeconds = totalSeconds; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
