package com.example.datadict.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Type detail with field list")
public class TypeDetail {

    @Schema(description = "Type basic info")
    private DictType type;

    @Schema(description = "Field definitions under this type")
    private List<DictField> fields;

    public TypeDetail(DictType type, List<DictField> fields) {
        this.type = type;
        this.fields = fields;
    }

    public DictType getType() { return type; }
    public List<DictField> getFields() { return fields; }
}
