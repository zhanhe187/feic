package com.zhanhe.servicedesk.web.controller.it;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.zhanhe.servicedesk.common.annotation.Log;
import com.zhanhe.servicedesk.common.core.controller.BaseController;
import com.zhanhe.servicedesk.common.core.domain.AjaxResult;
import com.zhanhe.servicedesk.common.core.page.TableDataInfo;
import com.zhanhe.servicedesk.common.enums.BusinessType;
import com.zhanhe.servicedesk.common.utils.poi.ExcelUtil;
import com.zhanhe.servicedesk.system.domain.ItAsset;
import com.zhanhe.servicedesk.system.service.IItAssetService;

@RestController
@RequestMapping("/it/asset")
public class ItAssetController extends BaseController
{
    @Autowired
    private IItAssetService assetService;

    @PreAuthorize("@ss.hasPermi('it:asset:list')")
    @GetMapping("/list")
    public TableDataInfo list(ItAsset asset)
    {
        startPage();
        List<ItAsset> list = assetService.selectItAssetList(asset);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('it:asset:export')")
    @Log(title = "IT 资产", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ItAsset asset)
    {
        List<ItAsset> list = assetService.selectItAssetList(asset);
        ExcelUtil<ItAsset> util = new ExcelUtil<ItAsset>(ItAsset.class);
        util.exportExcel(response, list, "it_asset");
    }

    @PreAuthorize("@ss.hasPermi('it:asset:import')")
    @Log(title = "IT 资产", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file) throws Exception
    {
        ExcelUtil<ItAsset> util = new ExcelUtil<ItAsset>(ItAsset.class);
        List<ItAsset> assetList = util.importExcel(file.getInputStream());
        return success(assetService.importAsset(assetList, getUsername()));
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<ItAsset> util = new ExcelUtil<ItAsset>(ItAsset.class);
        util.importTemplateExcel(response, "it_asset");
    }

    @PreAuthorize("@ss.hasPermi('it:asset:query')")
    @GetMapping("/{assetId}")
    public AjaxResult getInfo(@PathVariable Long assetId)
    {
        return success(assetService.selectItAssetById(assetId));
    }

    @PreAuthorize("@ss.hasPermi('it:asset:add')")
    @Log(title = "IT 资产", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ItAsset asset)
    {
        asset.setCreateBy(getUsername());
        return toAjax(assetService.insertItAsset(asset));
    }

    @PreAuthorize("@ss.hasPermi('it:asset:edit')")
    @Log(title = "IT 资产", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ItAsset asset)
    {
        asset.setUpdateBy(getUsername());
        return toAjax(assetService.updateItAsset(asset));
    }

    @PreAuthorize("@ss.hasPermi('it:asset:remove')")
    @Log(title = "IT 资产", businessType = BusinessType.DELETE)
    @DeleteMapping("/{assetIds}")
    public AjaxResult remove(@PathVariable Long[] assetIds)
    {
        return toAjax(assetService.deleteItAssetByIds(assetIds));
    }
}
