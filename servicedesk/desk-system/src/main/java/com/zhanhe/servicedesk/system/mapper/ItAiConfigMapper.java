package com.zhanhe.servicedesk.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zhanhe.servicedesk.system.domain.ItAiConfig;

public interface ItAiConfigMapper
{
    List<ItAiConfig> selectAll();

    int upsert(ItAiConfig config);

    String selectValue(@Param("configKey") String configKey);
}
