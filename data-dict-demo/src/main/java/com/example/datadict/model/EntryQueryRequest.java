package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Entry list query request.
 */
@Schema(description = "Entry list query request")
public class EntryQueryRequest {

    @NotBlank(message = "typeCode must not be blank")
    @Schema(description = "Dictionary type code", example = "PRODUCT")
    private String typeCode;

    @Schema(description = "Tag IDs (intersection filter). Empty/null means no tag filter.", example = "[\"tag001\", \"tag003\"]")
    private List<String> tagIds;

    @Min(1)
    @Schema(description = "Page number, 1-based", example = "1")
    private int pageNum = 1;

    @Min(1)
    @Schema(description = "Page size", example = "10")
    private int pageSize = 10;

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public List<String> getTagIds() { return tagIds; }
    public void setTagIds(List<String> tagIds) { this.tagIds = tagIds; }
    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getOffset() { return (pageNum - 1) * pageSize; }
}
