package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated result")
public class PageResult<T> {

    @Schema(description = "Total record count")
    private long total;

    @Schema(description = "Current page number")
    private int pageNum;

    @Schema(description = "Records per page")
    private int pageSize;

    @Schema(description = "Record list")
    private List<T> records;

    public PageResult(long total, int pageNum, int pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
    }

    public long getTotal() { return total; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public List<T> getRecords() { return records; }
}
