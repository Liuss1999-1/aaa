package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Dictionary entry")
public class DictEntry {

    @Schema(description = "Entry ID")
    private String id;

    @Schema(description = "Type code this entry belongs to", example = "PRODUCT")
    private String typeCode;

    @Schema(description = "Entry name", example = "iPhone 15")
    private String entryName;

    @Schema(description = "Dynamic field values")
    private Map<String, Object> fields;

    @Schema(description = "Associated tags")
    private List<DictTag> tags;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Last update time")
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getEntryName() { return entryName; }
    public void setEntryName(String entryName) { this.entryName = entryName; }
    public Map<String, Object> getFields() { return fields; }
    public void setFields(Map<String, Object> fields) { this.fields = fields; }
    public List<DictTag> getTags() { return tags; }
    public void setTags(List<DictTag> tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
