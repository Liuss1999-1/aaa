package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Type register / update request.
 */
@Schema(description = "Type register / update request")
public class TypeSaveRequest {

    @NotBlank(message = "typeCode must not be blank")
    @Schema(description = "Type code (unique identifier)", example = "PRODUCT")
    private String typeCode;

    @NotBlank(message = "typeName must not be blank")
    @Schema(description = "Display name", example = "Product")
    private String typeName;

    @NotBlank(message = "tableName must not be blank")
    @Schema(description = "Corresponding physical table name", example = "T_PRODUCT")
    private String tableName;

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
}
