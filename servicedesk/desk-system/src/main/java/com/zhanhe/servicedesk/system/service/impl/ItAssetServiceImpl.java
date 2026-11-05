package com.zhanhe.servicedesk.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhanhe.servicedesk.common.exception.ServiceException;
import com.zhanhe.servicedesk.common.utils.SecurityUtils;
import com.zhanhe.servicedesk.common.utils.StringUtils;
import com.zhanhe.servicedesk.system.domain.ItAsset;
import com.zhanhe.servicedesk.system.mapper.ItAssetMapper;
import com.zhanhe.servicedesk.system.service.IItAssetService;

@Service
public class ItAssetServiceImpl implements IItAssetService
{
    @Autowired
    private ItAssetMapper assetMapper;

    @Override
    public ItAsset selectItAssetById(Long assetId)
    {
        return assetMapper.selectItAssetById(assetId);
    }

    @Override
    public List<ItAsset> selectItAssetList(ItAsset asset)
    {
        if (!canManageAsset())
        {
            asset.setOwnerUser(SecurityUtils.getUsername());
        }
        return assetMapper.selectItAssetList(asset);
    }

    @Override
    public int insertItAsset(ItAsset asset)
    {
        validateAsset(asset);
        if (StringUtils.isEmpty(asset.getAssetStatus()))
        {
            asset.setAssetStatus("idle");
        }
        return assetMapper.insertItAsset(asset);
    }

    @Override
    public int updateItAsset(ItAsset asset)
    {
        validateAsset(asset);
        return assetMapper.updateItAsset(asset);
    }

    @Override
    public int deleteItAssetByIds(Long[] assetIds)
    {
        return assetMapper.deleteItAssetByIds(assetIds);
    }

    @Override
    public String importAsset(List<ItAsset> assetList, String operName)
    {
        if (StringUtils.isNull(assetList) || assetList.size() == 0)
        {
            throw new ServiceException("Import asset data cannot be empty");
        }
        int success = 0;
        for (ItAsset asset : assetList)
        {
            asset.setCreateBy(operName);
            insertItAsset(asset);
            success++;
        }
        return "Imported " + success + " assets";
    }

    private void validateAsset(ItAsset asset)
    {
        if (asset == null || StringUtils.isEmpty(asset.getAssetNo()))
        {
            throw new ServiceException("Asset no is required");
        }
        if (StringUtils.isEmpty(asset.getAssetType()))
        {
            throw new ServiceException("Asset type is required");
        }
        if (StringUtils.isEmpty(asset.getBrandModel()))
        {
            throw new ServiceException("Brand model is required");
        }
    }

    private boolean canManageAsset()
    {
        return SecurityUtils.isAdmin() || SecurityUtils.hasPermi("it:asset:edit") || SecurityUtils.hasPermi("it:asset:all");
    }
}
