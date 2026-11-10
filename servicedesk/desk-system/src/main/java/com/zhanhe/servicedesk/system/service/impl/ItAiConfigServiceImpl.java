package com.zhanhe.servicedesk.system.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.zhanhe.servicedesk.common.utils.StringUtils;
import com.zhanhe.servicedesk.system.domain.ItAiConfig;
import com.zhanhe.servicedesk.system.mapper.ItAiConfigMapper;
import com.zhanhe.servicedesk.system.service.IItAiConfigService;

@Service
public class ItAiConfigServiceImpl implements IItAiConfigService
{
    private final ItAiConfigMapper mapper;

    public ItAiConfigServiceImpl(ItAiConfigMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    public List<ItAiConfig> listAll()
    {
        return mapper.selectAll();
    }

    @Override
    public Map<String, String> asMap()
    {
        Map<String, String> map = new LinkedHashMap<>();
        for (ItAiConfig cfg : mapper.selectAll())
        {
            map.put(cfg.getConfigKey(), cfg.getConfigValue());
        }
        return map;
    }

    @Override
    public String getValue(String configKey)
    {
        try
        {
            return mapper.selectValue(configKey);
        }
        catch (Exception e)
        {
            // 表还没建好时不能把 AI 整体拖挂，交给上层回落 yml 默认值
            return null;
        }
    }

    @Override
    public int save(Map<String, String> values, String operator)
    {
        if (values == null || values.isEmpty())
        {
            return 0;
        }
        int updated = 0;
        Map<String, String> current = asMap();
        for (Map.Entry<String, String> e : values.entrySet())
        {
            String key = e.getKey();
            if (StringUtils.isEmpty(key) || !key.startsWith("ai."))
            {
                continue;
            }
            String value = e.getValue() == null ? "" : e.getValue().trim();
            String old = current.get(key);
            if (value.equals(old == null ? "" : old))
            {
                continue;
            }
            ItAiConfig cfg = new ItAiConfig();
            cfg.setConfigKey(key);
            cfg.setConfigValue(value);
            cfg.setUpdateBy(operator);
            mapper.upsert(cfg);
            updated++;
        }
        return updated;
    }
}
