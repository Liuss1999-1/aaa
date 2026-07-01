package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Entry create / update request.
 */
@Schema(description = "Entry create / update request")
public class EntrySaveRequest {

    @Schema(description = "Entry ID (required for update, omit for create)")
    private String id;

    @NotBlank(message = "typeCode must not be blank")
    @Schema(description = "Dictionary type code", example = "PRODUCT")
    private String typeCode;

    @NotBlank(message = "entryName must not be blank")
    @Schema(description = "Entry name", example = "iPhone 15")
    private String entryName;

    @NotNull(message = "fields must not be null")
    @Schema(description = "Dynamic fields keyed by fieldCode", example = "{\"price\": 6999, \"description\": \"An Apple phone\"}")
    private Map<String, Object> fields;

    @Schema(description = "Associated tag IDs")
    private List<String> tagIds;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getEntryName() { return entryName; }
    public void setEntryName(String entryName) { this.entryName = entryName; }
    public Map<String, Object> getFields() { return fields; }
    public void setFields(Map<String, Object> fields) { this.fields = fields; }
    public List<String> getTagIds() { return tagIds; }
    public void setTagIds(List<String> tagIds) { this.tagIds = tagIds; }
}
