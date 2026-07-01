package com.example.datadict.mapper;

import com.example.datadict.model.DictField;
import com.example.datadict.model.DictType;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Metadata Mapper: DICT_TYPE (type definitions) and DICT_FIELD (field definitions).
 */
@Mapper
public interface DictMetadataMapper {

    // ========== DICT_TYPE ==========

    @Select("SELECT * FROM DICT_TYPE WHERE TYPE_CODE = #{typeCode}")
    DictType findByTypeCode(String typeCode);

    @Select("SELECT * FROM DICT_TYPE WHERE ID = #{id}")
    DictType findTypeById(String id);

    @Select("SELECT * FROM DICT_TYPE ORDER BY CREATED_AT DESC")
    List<DictType> listTypes();

    @Insert("INSERT INTO DICT_TYPE (ID, TYPE_CODE, TYPE_NAME, TABLE_NAME) " +
            "VALUES (#{id}, #{typeCode}, #{typeName}, #{tableName})")
    int insertType(DictType type);

    @Update("UPDATE DICT_TYPE SET TYPE_NAME = #{typeName}, TABLE_NAME = #{tableName} WHERE ID = #{id}")
    int updateType(DictType type);

    @Delete("DELETE FROM DICT_TYPE WHERE TYPE_CODE = #{typeCode}")
    int deleteType(String typeCode);

    // ========== DICT_FIELD ==========

    @Select("SELECT * FROM DICT_FIELD WHERE TYPE_ID = #{typeId} ORDER BY SORT_ORDER")
    List<DictField> findFieldsByTypeId(String typeId);

    @Select("SELECT * FROM DICT_FIELD WHERE TYPE_ID = #{typeId} AND FIELD_CODE = #{fieldCode}")
    DictField findField(String typeId, String fieldCode);

    @Select("SELECT * FROM DICT_FIELD WHERE ID = #{id}")
    DictField findFieldById(String id);

    @Insert("INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED) " +
            "VALUES (#{id}, #{typeId}, #{fieldCode}, #{fieldName}, #{fieldType}, #{columnName}, #{sortOrder}, #{isRequired})")
    int insertField(DictField field);

    @Update("UPDATE DICT_FIELD SET FIELD_NAME = #{fieldName}, FIELD_TYPE = #{fieldType}, COLUMN_NAME = #{columnName}, " +
            "SORT_ORDER = #{sortOrder}, IS_REQUIRED = #{isRequired} WHERE ID = #{id}")
    int updateField(DictField field);

    @Delete("DELETE FROM DICT_FIELD WHERE TYPE_ID = #{typeId} AND FIELD_CODE = #{fieldCode}")
    int deleteField(String typeId, String fieldCode);

    @Delete("DELETE FROM DICT_FIELD WHERE TYPE_ID = #{typeId}")
    int deleteFieldsByTypeId(String typeId);
}
