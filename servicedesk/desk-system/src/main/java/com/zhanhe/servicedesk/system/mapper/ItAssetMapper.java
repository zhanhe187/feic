package com.zhanhe.servicedesk.system.mapper;

import java.util.List;
import com.zhanhe.servicedesk.system.domain.ItAsset;

public interface ItAssetMapper
{
    ItAsset selectItAssetById(Long assetId);

    List<ItAsset> selectItAssetList(ItAsset asset);

    int insertItAsset(ItAsset asset);

    int updateItAsset(ItAsset asset);

    int deleteItAssetByIds(Long[] assetIds);

    int deleteItAssetById(Long assetId);
}
