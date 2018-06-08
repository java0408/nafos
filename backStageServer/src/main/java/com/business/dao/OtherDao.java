package com.business.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;
@Repository
public interface OtherDao {
    int updateSystemInfoByName(@Param("dataBaseName") String dataBaseName, @Param("gameName") String gameName, @Param("configName") String configName, @Param("isOpen") Boolean isOpen);

    Map selectSomeSystemInfoByName(@Param("dataBaseName") String dataBaseName, @Param("gameName") String gameName, @Param("configName") String configName);
}
