package com.example.datadict.controller;

import com.example.datadict.model.*;
import com.example.datadict.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dictionary Management", description = "CRUD for types, fields, entries, and tags")
@RestController
@RequestMapping("/api/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    // ==================== Type Management ====================

    @Operation(summary = "Register a new dictionary type")
    @PostMapping("/type")
    public ApiResponse<DictType> createType(@Valid @RequestBody TypeSaveRequest req) {
        return ApiResponse.ok(dictService.createType(req));
    }

    @Operation(summary = "Update an existing dictionary type")
    @PutMapping("/type")
    public ApiResponse<DictType> updateType(@Valid @RequestBody TypeSaveRequest req) {
        return ApiResponse.ok(dictService.updateType(req));
    }

    @Operation(summary = "Delete a dictionary type and cascade-delete entries, tags, and field definitions")
    @DeleteMapping("/type/{typeCode}")
    public ApiResponse<Void> deleteType(@Parameter(description = "type code") @PathVariable String typeCode) {
        dictService.deleteType(typeCode);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Get type detail with field definitions")
    @GetMapping("/type/{typeCode}")
    public ApiResponse<TypeDetail> getType(@Parameter(description = "type code") @PathVariable String typeCode) {
        return ApiResponse.ok(dictService.getType(typeCode));
    }

    @Operation(summary = "List all dictionary types")
    @GetMapping("/type")
    public ApiResponse<List<DictType>> listTypes() {
        return ApiResponse.ok(dictService.listTypes());
    }

    // ==================== Field Management ====================

    @Operation(summary = "Add a field definition to a type")
    @PostMapping("/type/{typeCode}/field")
    public ApiResponse<DictField> addField(
            @Parameter(description = "type code") @PathVariable String typeCode,
            @Valid @RequestBody FieldSaveRequest req) {
        return ApiResponse.ok(dictService.addField(typeCode, req));
    }

    @Operation(summary = "Update a field definition")
    @PutMapping("/type/{typeCode}/field")
    public ApiResponse<DictField> updateField(
            @Parameter(description = "type code") @PathVariable String typeCode,
            @Valid @RequestBody FieldSaveRequest req) {
        return ApiResponse.ok(dictService.updateField(typeCode, req));
    }

    @Operation(summary = "Delete a field definition")
    @DeleteMapping("/type/{typeCode}/field/{fieldCode}")
    public ApiResponse<Void> deleteField(
            @Parameter(description = "type code") @PathVariable String typeCode,
            @Parameter(description = "field code") @PathVariable String fieldCode) {
        dictService.deleteField(typeCode, fieldCode);
        return ApiResponse.ok(null);
    }

    // ==================== Entry CRUD ====================

    @Operation(summary = "Create an entry", description = "Routes to the correct physical table via typeCode metadata; supports multi-tag association")
    @PostMapping("/entry")
    public ApiResponse<DictEntry> createEntry(@Valid @RequestBody EntrySaveRequest req) {
        return ApiResponse.ok(dictService.createEntry(req));
    }

    @Operation(summary = "Update an entry", description = "id is required; tags use delete-then-insert strategy")
    @PutMapping("/entry")
    public ApiResponse<DictEntry> updateEntry(@Valid @RequestBody EntrySaveRequest req) {
        return ApiResponse.ok(dictService.updateEntry(req));
    }

    @Operation(summary = "Delete an entry")
    @DeleteMapping("/entry/{typeCode}/{id}")
    public ApiResponse<Void> deleteEntry(
            @Parameter(description = "type code") @PathVariable String typeCode,
            @Parameter(description = "entry ID") @PathVariable String id) {
        dictService.deleteEntry(typeCode, id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Get a single entry by ID")
    @GetMapping("/entry/{typeCode}/{id}")
    public ApiResponse<DictEntry> getEntry(
            @Parameter(description = "type code") @PathVariable String typeCode,
            @Parameter(description = "entry ID") @PathVariable String id) {
        return ApiResponse.ok(dictService.getEntry(typeCode, id));
    }

    @Operation(summary = "Query entries with pagination", description = "Supports tag intersection filtering. Empty tagIds means no tag filter")
    @PostMapping("/entry/query")
    public ApiResponse<PageResult<DictEntry>> queryEntries(@Valid @RequestBody EntryQueryRequest req) {
        return ApiResponse.ok(dictService.queryEntries(req));
    }

    // ==================== Tag Management ====================

    @Operation(summary = "Create a tag")
    @PostMapping("/tag")
    public ApiResponse<DictTag> createTag(@RequestBody DictTag tag) {
        return ApiResponse.ok(dictService.createTag(tag));
    }

    @Operation(summary = "Update a tag")
    @PutMapping("/tag")
    public ApiResponse<DictTag> updateTag(@RequestBody DictTag tag) {
        return ApiResponse.ok(dictService.updateTag(tag));
    }

    @Operation(summary = "Delete a tag")
    @DeleteMapping("/tag/{id}")
    public ApiResponse<Void> deleteTag(@Parameter(description = "tag ID") @PathVariable String id) {
        dictService.deleteTag(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "List all tags")
    @GetMapping("/tag")
    public ApiResponse<List<DictTag>> listTags() {
        return ApiResponse.ok(dictService.listTags());
    }

    @Operation(summary = "List tags used under a given type")
    @GetMapping("/tag/{typeCode}")
    public ApiResponse<List<DictTag>> listTagsByType(@Parameter(description = "type code") @PathVariable String typeCode) {
        return ApiResponse.ok(dictService.listTagsByType(typeCode));
    }
}
