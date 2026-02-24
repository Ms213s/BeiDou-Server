package org.gms.controller;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.gms.constants.api.ApiConstant;
import org.gms.dao.entity.ModifiedCashItemDO;
import org.gms.model.dto.AddCashShopItemReqDTO;
import org.gms.model.dto.AddCashShopItemRtnDTO;
import org.gms.model.dto.CashShopBatchOnSaleReqDTO;
import org.gms.model.dto.CashShopSearchRtnDTO;
import org.gms.model.dto.ItemInfoRtnDTO;
import org.gms.model.dto.ResultBody;
import org.gms.model.dto.SubmitBody;
import org.gms.model.pojo.CashCategory;
import org.gms.service.CashShopService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cashShop")
@AllArgsConstructor
public class CashShopController {
    private final CashShopService cashShopService;

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "获取商城全部分类")
    @GetMapping("/" + ApiConstant.LATEST + "/getAllCategoryList")
    public ResultBody<List<CashCategory>> getAllCategoryList() {
        return ResultBody.success(cashShopService.getAllCategoryList());
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "分页分类查询商品列表")
    @PostMapping("/" + ApiConstant.LATEST + "/getCommodityByCategory")
    public ResultBody<Page<CashShopSearchRtnDTO>> getCommodityByCategory(@RequestBody SubmitBody<CashCategory> request) {
        return ResultBody.success(cashShopService.getCommodityByCategory(request.getData()));
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "根据sn查询商品明细")
    @GetMapping("/" + ApiConstant.LATEST + "/getCommodityBySn/{sn}")
    public ResultBody<CashShopSearchRtnDTO> getCommodityBySn(@PathVariable("sn") Integer sn) {
        return ResultBody.success(cashShopService.getCommodityBySn(sn));
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "上架商品")
    @PostMapping("/" + ApiConstant.LATEST + "/onSale")
    public ResultBody<Object> onSale(@RequestBody SubmitBody<ModifiedCashItemDO> request) {
        request.getData().setOnSale(1);
        cashShopService.changeOnSale(request.getData());
        return ResultBody.success();
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "下架商品")
    @PostMapping("/" + ApiConstant.LATEST + "/offSale")
    public ResultBody<Object> offSale(@RequestBody SubmitBody<ModifiedCashItemDO> request) {
        request.getData().setOnSale(0);
        cashShopService.changeOnSale(request.getData());
        return ResultBody.success();
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "批量上架商品")
    @PostMapping("/" + ApiConstant.LATEST + "/batchOnSale")
    public ResultBody<Object> batchOnSale(@RequestBody SubmitBody<CashShopBatchOnSaleReqDTO> request) {
        cashShopService.batchChangeOnSale(request.getData());
        return ResultBody.success();
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "根据物品ID获取物品基础信息")
    @GetMapping("/" + ApiConstant.LATEST + "/getItemInfoById/{itemId}")
    public ResultBody<ItemInfoRtnDTO> getItemInfoById(@PathVariable("itemId") Integer itemId) {
        return ResultBody.success(cashShopService.getItemInfoById(itemId));
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "添加商品到商城")
    @PostMapping("/" + ApiConstant.LATEST + "/addCashShopItem")
    public ResultBody<AddCashShopItemRtnDTO> addCashShopItem(@RequestBody SubmitBody<AddCashShopItemReqDTO> request) {
        return ResultBody.success(cashShopService.addCashShopItem(request.getData()));
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "更新商品信息")
    @PostMapping("/" + ApiConstant.LATEST + "/updateCashShopItem")
    public ResultBody<Object> updateCashShopItem(@RequestBody SubmitBody<AddCashShopItemReqDTO> request) {
        cashShopService.updateCashShopItem(request.getData());
        return ResultBody.success();
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "删除/还原商品")
    @PostMapping("/" + ApiConstant.LATEST + "/deleteCashShopItem")
    public ResultBody<Object> deleteCashShopItem(@RequestBody SubmitBody<Integer> request) {
        cashShopService.deleteCashShopItem(request.getData());
        return ResultBody.success();
    }

    @Tag(name = "/cashShop/" + ApiConstant.LATEST)
    @Operation(summary = "批量删除/还原商品")
    @PostMapping("/" + ApiConstant.LATEST + "/batchDeleteCashShopItem")
    public ResultBody<Object> batchDeleteCashShopItem(@RequestBody SubmitBody<List<Integer>> request) {
        cashShopService.batchDeleteCashShopItem(request.getData());
        return ResultBody.success();
    }
}
