package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Tag")
public class DictTag {

    @Schema(description = "Primary key (UUID)")
    private String id;

    @Schema(description = "Tag name", example = "Hot")
    private String tagName;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
