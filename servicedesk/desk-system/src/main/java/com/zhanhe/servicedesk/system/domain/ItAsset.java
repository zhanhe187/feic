package com.zhanhe.servicedesk.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhanhe.servicedesk.common.annotation.Excel;
import com.zhanhe.servicedesk.common.core.domain.BaseEntity;

public class ItAsset extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long assetId;

    @Excel(name = "Asset No")
    private String assetNo;

    @Excel(name = "Asset Type")
    private String assetType;

    @Excel(name = "Brand Model")
    private String brandModel;

    @Excel(name = "Owner")
    private String ownerUser;

    @Excel(name = "Status")
    private String assetStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "Receive Time", width = 30, dateFormat = "yyyy-MM-dd")
    private Date receiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "Return Time", width = 30, dateFormat = "yyyy-MM-dd")
    private Date returnTime;

    public Long getAssetId()
    {
        return assetId;
    }

    public void setAssetId(Long assetId)
    {
        this.assetId = assetId;
    }

    public String getAssetNo()
    {
        return assetNo;
    }

    public void setAssetNo(String assetNo)
    {
        this.assetNo = assetNo;
    }

    public String getAssetType()
    {
        return assetType;
    }

    public void setAssetType(String assetType)
    {
        this.assetType = assetType;
    }

    public String getBrandModel()
    {
        return brandModel;
    }

    public void setBrandModel(String brandModel)
    {
        this.brandModel = brandModel;
    }

    public String getOwnerUser()
    {
        return ownerUser;
    }

    public void setOwnerUser(String ownerUser)
    {
        this.ownerUser = ownerUser;
    }

    public String getAssetStatus()
    {
        return assetStatus;
    }

    public void setAssetStatus(String assetStatus)
    {
        this.assetStatus = assetStatus;
    }

    public Date getReceiveTime()
    {
        return receiveTime;
    }

    public void setReceiveTime(Date receiveTime)
    {
        this.receiveTime = receiveTime;
    }

    public Date getReturnTime()
    {
        return returnTime;
    }

    public void setReturnTime(Date returnTime)
    {
        this.returnTime = returnTime;
    }
}
