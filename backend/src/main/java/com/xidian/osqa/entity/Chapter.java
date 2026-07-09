package com.xidian.osqa.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("chapter")
public class Chapter {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("video_set_id")
    private Long videoSetId;
    private String title;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private java.util.List<Section> sections;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVideoSetId() { return videoSetId; }
    public void setVideoSetId(Long videoSetId) { this.videoSetId = videoSetId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public java.util.List<Section> getSections() { return sections; }
    public void setSections(java.util.List<Section> sections) { this.sections = sections; }
}
