package com.example.datadict.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * Dynamic Mapper: uses Provider classes to operate on physical data tables.
 * All methods return Maps — column names are resolved at runtime from DICT_FIELD metadata.
 */
@Mapper
public interface DictDynamicMapper {

    @InsertProvider(type = com.example.datadict.provider.InsertProvider.class, method = "insert")
    @Options(useGeneratedKeys = false)
    int insert(Map<String, Object> params);

    @UpdateProvider(type = com.example.datadict.provider.UpdateProvider.class, method = "update")
    int update(Map<String, Object> params);

    @DeleteProvider(type = com.example.datadict.provider.DeleteProvider.class, method = "deleteById")
    int deleteById(Map<String, Object> params);

    @DeleteProvider(type = com.example.datadict.provider.DeleteProvider.class, method = "deleteByTypeId")
    int deleteAllByTable(Map<String, Object> params);

    @SelectProvider(type = com.example.datadict.provider.QueryProvider.class, method = "findById")
    Map<String, Object> findById(Map<String, Object> params);

    @SelectProvider(type = com.example.datadict.provider.QueryProvider.class, method = "queryPage")
    List<Map<String, Object>> queryPage(Map<String, Object> params);

    @SelectProvider(type = com.example.datadict.provider.QueryProvider.class, method = "countPage")
    long countPage(Map<String, Object> params);
}
