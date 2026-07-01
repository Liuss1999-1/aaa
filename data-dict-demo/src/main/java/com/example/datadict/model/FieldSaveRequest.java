package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Field definition request.
 */
@Schema(description = "Field definition request")
public class FieldSaveRequest {

    @NotBlank(message = "fieldCode must not be blank")
    @Schema(description = "Field code (API layer identifier)", example = "price")
    private String fieldCode;

    @NotBlank(message = "fieldName must not be blank")
    @Schema(description = "Field display name", example = "Price")
    private String fieldName;

    @NotBlank(message = "fieldType must not be blank")
    @Schema(description = "Field type: TEXT / NUMBER / DATE / BOOL", example = "NUMBER")
    private String fieldType;

    @NotBlank(message = "columnName must not be blank")
    @Schema(description = "Corresponding physical column name", example = "C_PRICE")
    private String columnName;

    @Schema(description = "Sort order")
    private int sortOrder;

    @Schema(description = "Required: 1-yes, 0-no", example = "1")
    private String isRequired = "0";

    public String getFieldCode() { return fieldCode; }
    public void setFieldCode(String fieldCode) { this.fieldCode = fieldCode; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getIsRequired() { return isRequired; }
    public void setIsRequired(String isRequired) { this.isRequired = isRequired; }
}
