package com.example.datadict.service;

import com.example.datadict.model.*;

import java.util.List;

/**
 * Dictionary Service — CRUD for types, fields, entries, and tags.
 */
public interface DictService {

    // -- Type management --
    DictType createType(TypeSaveRequest req);
    DictType updateType(TypeSaveRequest req);
    void deleteType(String typeCode);
    TypeDetail getType(String typeCode);
    List<DictType> listTypes();

    // -- Field management --
    DictField addField(String typeCode, FieldSaveRequest req);
    DictField updateField(String typeCode, FieldSaveRequest req);
    void deleteField(String typeCode, String fieldCode);

    // -- Entry CRUD --
    DictEntry createEntry(EntrySaveRequest req);
    DictEntry updateEntry(EntrySaveRequest req);
    void deleteEntry(String typeCode, String id);
    DictEntry getEntry(String typeCode, String id);
    PageResult<DictEntry> queryEntries(EntryQueryRequest req);

    // -- Tag management --
    DictTag createTag(DictTag tag);
    DictTag updateTag(DictTag tag);
    void deleteTag(String id);
    List<DictTag> listTags();
    List<DictTag> listTagsByType(String typeCode);
}
