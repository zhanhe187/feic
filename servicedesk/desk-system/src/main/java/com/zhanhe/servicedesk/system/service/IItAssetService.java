package com.zhanhe.servicedesk.system.service;

import java.util.List;
import com.zhanhe.servicedesk.system.domain.ItAsset;

public interface IItAssetService
{
    ItAsset selectItAssetById(Long assetId);

    List<ItAsset> selectItAssetList(ItAsset asset);

    int insertItAsset(ItAsset asset);

    int updateItAsset(ItAsset asset);

    int deleteItAssetByIds(Long[] assetIds);

    String importAsset(List<ItAsset> assetList, String operName);
}
