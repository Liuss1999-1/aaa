package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Field definition")
public class DictField {

    @Schema(description = "Primary key (UUID)")
    private String id;

    @Schema(description = "Owning type ID")
    private String typeId;

    @Schema(description = "Field code", example = "price")
    private String fieldCode;

    @Schema(description = "Field display name", example = "Price")
    private String fieldName;

    @Schema(description = "Field type: TEXT / NUMBER / DATE / BOOL", example = "NUMBER")
    private String fieldType;

    @Schema(description = "Physical column name", example = "C_PRICE")
    private String columnName;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Required: 1-yes, 0-no")
    private String isRequired;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTypeId() { return typeId; }
    public void setTypeId(String typeId) { this.typeId = typeId; }
    public String getFieldCode() { return fieldCode; }
    public void setFieldCode(String fieldCode) { this.fieldCode = fieldCode; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getIsRequired() { return isRequired; }
    public void setIsRequired(String isRequired) { this.isRequired = isRequired; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isRequired() {
        return "1".equals(isRequired);
    }
}
