package com.zhanhe.servicedesk.system.service;

import java.util.List;
import java.util.Map;

import com.zhanhe.servicedesk.system.domain.ItAiConfig;

public interface IItAiConfigService
{
    /** 全部配置（含说明） */
    List<ItAiConfig> listAll();

    /** 键值对视图，供运行时读取 */
    Map<String, String> asMap();

    /** 读单个值，缺失返回 null */
    String getValue(String configKey);

    /** 批量保存，只更新调用方提交的键 */
    int save(Map<String, String> values, String operator);
}
