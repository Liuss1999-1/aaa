package com.example.datadict.mapper;

import com.example.datadict.model.DictTag;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * Tag and entry-tag association Mapper.
 */
@Mapper
public interface DictTagMapper {

    // ========== TAG CRUD ==========

    @Select("SELECT * FROM DICT_TAG ORDER BY CREATED_AT DESC")
    List<DictTag> listTags();

    @Select("SELECT * FROM DICT_TAG WHERE ID = #{id}")
    DictTag findById(String id);

    @Insert("INSERT INTO DICT_TAG (ID, TAG_NAME, CREATED_AT) VALUES (#{id}, #{tagName}, CURRENT_TIMESTAMP)")
    int insert(DictTag tag);

    @Update("UPDATE DICT_TAG SET TAG_NAME = #{tagName} WHERE ID = #{id}")
    int update(DictTag tag);

    @Delete("DELETE FROM DICT_TAG WHERE ID = #{id}")
    int delete(String id);

    // ========== ENTRY-TAG Associations ==========

    @Insert("INSERT INTO DICT_ENTRY_TAG (ID, TYPE_ID, ENTRY_ID, TAG_ID) VALUES (#{id}, #{typeId}, #{entryId}, #{tagId})")
    int insertEntryTag(String id, String typeId, String entryId, String tagId);

    @Delete("DELETE FROM DICT_ENTRY_TAG WHERE TYPE_ID = #{typeId} AND ENTRY_ID = #{entryId}")
    int deleteByEntry(String typeId, String entryId);

    @Delete("DELETE FROM DICT_ENTRY_TAG WHERE TYPE_ID = #{typeId}")
    int deleteByTypeId(String typeId);

    @Select("SELECT t.* FROM DICT_TAG t INNER JOIN DICT_ENTRY_TAG et ON t.ID = et.TAG_ID " +
            "WHERE et.TYPE_ID = #{typeId} AND et.ENTRY_ID = #{entryId}")
    List<DictTag> findTagsByEntry(String typeId, String entryId);

    @Select("SELECT DISTINCT t.* FROM DICT_TAG t INNER JOIN DICT_ENTRY_TAG et ON t.ID = et.TAG_ID " +
            "WHERE et.TYPE_ID = #{typeId}")
    List<DictTag> findTagsByTypeId(String typeId);

    @Select("SELECT DISTINCT t.* FROM DICT_TAG t INNER JOIN DICT_ENTRY_TAG et ON t.ID = et.TAG_ID " +
            "WHERE et.TYPE_ID = #{typeId} AND et.ENTRY_ID IN (SELECT et2.ENTRY_ID FROM DICT_ENTRY_TAG et2 " +
            "WHERE et2.TYPE_ID = #{typeId} AND et2.TAG_ID IN (#{tagIds}))")
    List<DictTag> findTagsByEntryIds(String typeId, List<String> entryIds);

    /**
     * Batch-find tags for multiple entries to avoid N+1 queries.
     */
    @Select("<script>" +
            "SELECT et.ENTRY_ID, t.ID, t.TAG_NAME, t.CREATED_AT " +
            "FROM DICT_TAG t INNER JOIN DICT_ENTRY_TAG et ON t.ID = et.TAG_ID " +
            "WHERE et.TYPE_ID = #{typeId} " +
            "AND et.ENTRY_ID IN " +
            "<foreach collection='entryIds' item='eid' open='(' separator=',' close=')'>" +
            "#{eid}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> findTagsForEntries(String typeId, List<String> entryIds);
}
