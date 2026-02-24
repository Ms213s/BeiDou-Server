package org.gms.service;

import com.mybatisflex.core.paginate.Page;
import lombok.AllArgsConstructor;
import org.gms.constants.string.CategoryType;
import org.gms.dao.entity.ModifiedCashItemDO;
import org.gms.dao.mapper.ModifiedCashItemMapper;
import org.gms.exception.BizException;
import org.gms.model.dto.AddCashShopItemReqDTO;
import org.gms.model.dto.AddCashShopItemRtnDTO;
import org.gms.model.dto.CashShopBatchOnSaleReqDTO;
import org.gms.model.dto.CashShopSearchRtnDTO;
import org.gms.model.dto.ItemInfoRtnDTO;
import org.gms.model.pojo.CashCategory;
import org.gms.provider.Data;
import org.gms.provider.DataProvider;
import org.gms.provider.DataProviderFactory;
import org.gms.provider.DataTool;
import org.gms.provider.wz.WZFiles;
import org.gms.server.CashShop;
import org.gms.server.ItemInformationProvider;
import org.gms.util.BasePageUtil;
import org.gms.util.I18nUtil;
import org.gms.util.RequireUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@AllArgsConstructor
public class CashShopService {
    private final ModifiedCashItemMapper modifiedCashItemMapper;

    public List<ModifiedCashItemDO> loadAllModifiedCashItems() {
        return modifiedCashItemMapper.selectAll();
    }

    public List<CashCategory> getAllCategoryList() {
        DataProvider etc = DataProviderFactory.getDataProvider(WZFiles.ETC);
        List<CashCategory> cashCategoryList = new ArrayList<>();
        for (Data item : etc.getData("Category.img").getChildren()) {
            int id = DataTool.getIntConvert("Category", item);
            int subId = DataTool.getIntConvert("CategorySub", item);
            String subName = DataTool.getString("Name", item);
            String name = CategoryType.toName(id);
            cashCategoryList.add(CashCategory.builder().id(id).name(name).subId(subId).subName(subName).build());
        }
        return cashCategoryList;
    }

    public Page<CashShopSearchRtnDTO> getCommodityByCategory(CashCategory data) {
        RequireUtil.requireNotNull(data.getId(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "id"));
        RequireUtil.requireNotNull(data.getSubId(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "subId"));

        CashCategory cashCategory = getCategory(data.getId(), data.getSubId());
        // 默认每页10条
        if (data.getPageSize() == null || data.getPageSize() <= 0) {
            data.setPageSize(10);
        }

        final String prefix = data.getId() + String.format("%02d", data.getSubId());
        // wz中的物品
        List<CashShopSearchRtnDTO> allCashItems = new ArrayList<>(CashShop.CashItemFactory.getItems().values().stream()
                // 按分类过滤
                .filter(cashItem -> String.valueOf(cashItem.getSn()).startsWith(prefix))
                .map(cashItem -> fromCashItem(cashCategory, cashItem, false, false))
                .toList());
        // 数据库中的物品
        List<ModifiedCashItemDO> dbCashItems = CashShop.CashItemFactory.getModifiedCashItems().values().stream()
                // 按分类过滤
                .filter(modifiedCashItemDO -> String.valueOf(modifiedCashItemDO.getSn()).startsWith(prefix))
                .toList();
        
        // 先创建一个Map来存储wz商品，方便查找
        Map<Integer, CashShopSearchRtnDTO> itemMap = new HashMap<>();
        allCashItems.forEach(item -> itemMap.put(item.getSn(), item));
        
        // 以数据库为准更新可能更新的字段
        for (ModifiedCashItemDO dbCashItem : dbCashItems) {
            CashShopSearchRtnDTO existingItem = itemMap.get(dbCashItem.getSn());
            if (existingItem != null) {
                // 修改自WZ的商品
                existingItem.setIsDbItem(true);
                existingItem.setIsPureDbItem(false);
                setDbItemValue(existingItem, dbCashItem);
            } else {
                // 纯新增的商品（数据库有但WZ没有）
                CashShopSearchRtnDTO newItem = fromCashItem(cashCategory, dbCashItem, true, true);
                newItem.setIsPureDbItem(true);
                allCashItems.add(newItem);
                itemMap.put(dbCashItem.getSn(), newItem);
            }
        }

        // 按其他条件过滤
        List<CashShopSearchRtnDTO> filteredItems = allCashItems.stream().filter(item ->
                // 上架状态
                (data.getOnSale() == null || Objects.equals(data.getOnSale(), item.getOnSale() != null && item.getOnSale() == 1))
                        // 物品id
                        && (data.getItemId() == null || data.getItemId().equals(item.getItemId()))
                        // 数据库商品筛选
                        && (data.getIsDbItem() == null || Objects.equals(data.getIsDbItem(), item.getIsDbItem()))
        ).toList();

        // 现在需要批量去set wzCashItems中的itemName值
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        filteredItems.forEach(cashItem -> {
            cashItem.setItemName(ii.getName(cashItem.getItemId()));
        });


        // 排序是否正确？ 猜测按照Priority降序 ItemId升序排列
        return BasePageUtil.create(filteredItems, data)
                .sorted(Comparator.comparing(CashShopSearchRtnDTO::getPriority).reversed().thenComparing(CashShopSearchRtnDTO::getItemId))
                .page();
    }

    public CashShopSearchRtnDTO getCommodityBySn(Integer sn) {
        RequireUtil.requireNotNull(sn, I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "sn"));
        String snStr = String.valueOf(sn);
        int id = Integer.parseInt(snStr.substring(0, 1));
        int subId = Integer.parseInt(snStr.substring(1, 3));
        CashCategory cashCategory = getCategory(id, subId);
        
        ModifiedCashItemDO cashItem = CashShop.CashItemFactory.getWzItem(sn);
        ModifiedCashItemDO dbCashItem = CashShop.CashItemFactory.getModifiedCashItems().get(sn);
        
        if (cashItem == null && dbCashItem == null) {
            throw new BizException(I18nUtil.getExceptionMessage("UNKNOWN_PARAMETER_VALUE", "sn", sn));
        }
        
        ModifiedCashItemDO itemToUse = cashItem != null ? cashItem : dbCashItem;
        boolean isDbItem = dbCashItem != null;
        boolean isPureDbItem = dbCashItem != null && cashItem == null;
        CashShopSearchRtnDTO rtnDTO = fromCashItem(cashCategory, itemToUse, isDbItem, isPureDbItem);

        if (dbCashItem != null) {
            setDbItemValue(rtnDTO, dbCashItem);
        }

        return rtnDTO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeOnSale(ModifiedCashItemDO data) {
        RequireUtil.requireNotNull(data.getSn(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "sn"));
        ModifiedCashItemDO cashItem = CashShop.CashItemFactory.getWzItem(data.getSn());
        ModifiedCashItemDO dbCashItem = CashShop.CashItemFactory.getModifiedCashItems().get(data.getSn());
        
        modifiedCashItemMapper.deleteById(data.getSn());

        // 如果是下架，直接插入或更新除状态外所有值为null
        if (data.getOnSale() != null && data.getOnSale() != 1) {
            boolean isSelling = (cashItem != null && cashItem.isSelling()) || (dbCashItem != null && dbCashItem.getOnSale() != null && dbCashItem.getOnSale() == 1);
            if (isSelling) {
                modifiedCashItemMapper.insertSelective(ModifiedCashItemDO.builder().sn(data.getSn()).onSale(0).build());
            }
            CashShop.CashItemFactory.loadAllModifiedCashItems();
            return;
        }
        
        ModifiedCashItemDO itemToCompare = cashItem != null ? cashItem : dbCashItem;
        if (itemToCompare != null) {
            if (Objects.equals(itemToCompare.getItemId(), data.getItemId())) {
                data.setItemId(null);
            }
            if (Objects.equals(itemToCompare.getPrice(), data.getPrice())) {
                data.setPrice(null);
            }
            if (Objects.equals(itemToCompare.getPeriod(), data.getPeriod())) {
                data.setPeriod(null);
            }
            if (Objects.equals(itemToCompare.getPriority(), data.getPriority())) {
                data.setPriority(null);
            }
            if (Objects.equals(itemToCompare.getCount(), data.getCount())) {
                data.setCount(null);
            }
            if (Objects.equals(itemToCompare.getOnSale(), data.getOnSale())) {
                data.setOnSale(null);
            }
        }
        
        modifiedCashItemMapper.insertSelective(data);
        CashShop.CashItemFactory.loadAllModifiedCashItems();
    }

    private CashCategory getCategory(Integer id, Integer subId) {
        return CashShop.CashItemFactory.getCashCategories().stream()
                .filter(cc -> Objects.equals(cc.getId(), id) && Objects.equals(cc.getSubId(), subId))
                .findFirst()
                .orElseThrow(() -> new BizException(I18nUtil.getExceptionMessage("CashShopService.getByCategory.exception1")));
    }

    private CashShopSearchRtnDTO fromCashItem(CashCategory cashCategory, ModifiedCashItemDO cashItem, boolean isDbItem, boolean isPureDbItem) {
        return CashShopSearchRtnDTO.builder()
                .categoryId(cashCategory.getId())
                .categoryName(cashCategory.getName())
                .subcategoryId(cashCategory.getSubId())
                .subcategoryName(cashCategory.getSubName())
                .sn(cashItem.getSn())
                .itemId(cashItem.getItemId())
                .price(cashItem.getPrice())
                .defaultPrice(cashItem.getPrice())
                .period(cashItem.getPeriod())
                .defaultPeriod(cashItem.getPeriod())
                .priority(cashItem.getPriority())
                .defaultPriority(cashItem.getPriority())
                .count(cashItem.getCount())
                .defaultCount(cashItem.getCount())
                .onSale(cashItem.getOnSale())
                .defaultOnSale(cashItem.getOnSale())
                .bonus(cashItem.getBonus())
                .defaultBonus(cashItem.getBonus())
                .maplePoint(cashItem.getMaplePoint())
                .defaultMaplePoint(cashItem.getMaplePoint())
                .meso(cashItem.getMeso())
                .defaultMeso(cashItem.getMeso())
                .forPremiumUser(cashItem.getForPremiumUser())
                .defaultForPremiumUser(cashItem.getForPremiumUser())
                .gender(cashItem.getCommodityGender())
                .defaultGender(cashItem.getCommodityGender())
                .clz(cashItem.getClz())
                .defaultClz(cashItem.getClz())
                .limit(cashItem.getLimit())
                .defaultLimit(cashItem.getLimit())
                .pbCash(cashItem.getPbCash())
                .defaultPBCash(cashItem.getPbCash())
                .pbPoint(cashItem.getPbPoint())
                .defaultPBPoint(cashItem.getPbPoint())
                .pbGift(cashItem.getPbGift())
                .defaultPBGift(cashItem.getPbGift())
                .packageSn(cashItem.getPackageSn())
                .defaultPackageSn(cashItem.getPackageSn())
                .isDbItem(isDbItem)
                .isPureDbItem(isPureDbItem)
                .build();
    }

    private void setDbItemValue(CashShopSearchRtnDTO rtnDTO, ModifiedCashItemDO dbCashItem) {
        rtnDTO.setItemId(Optional.ofNullable(dbCashItem.getItemId()).orElse(rtnDTO.getItemId()));
        rtnDTO.setPrice(Optional.ofNullable(dbCashItem.getPrice()).orElse(rtnDTO.getPrice()));
        rtnDTO.setPeriod(Optional.ofNullable(dbCashItem.getPeriod()).orElse(rtnDTO.getPeriod()));
        rtnDTO.setPriority(Optional.ofNullable(dbCashItem.getPriority()).orElse(rtnDTO.getPriority()));
        rtnDTO.setCount(Optional.ofNullable(dbCashItem.getCount()).orElse(rtnDTO.getCount()));
        rtnDTO.setOnSale(Optional.ofNullable(dbCashItem.getOnSale()).orElse(rtnDTO.getOnSale()));
        rtnDTO.setBonus(Optional.ofNullable(dbCashItem.getBonus()).orElse(rtnDTO.getBonus()));
        rtnDTO.setMaplePoint(Optional.ofNullable(dbCashItem.getMaplePoint()).orElse(rtnDTO.getMaplePoint()));
        rtnDTO.setMeso(Optional.ofNullable(dbCashItem.getMeso()).orElse(rtnDTO.getMeso()));
        rtnDTO.setForPremiumUser(Optional.ofNullable(dbCashItem.getForPremiumUser()).orElse(rtnDTO.getForPremiumUser()));
        rtnDTO.setGender(Optional.ofNullable(dbCashItem.getCommodityGender()).orElse(rtnDTO.getGender()));
        rtnDTO.setClz(Optional.ofNullable(dbCashItem.getClz()).orElse(rtnDTO.getClz()));
        rtnDTO.setLimit(Optional.ofNullable(dbCashItem.getLimit()).orElse(rtnDTO.getLimit()));
        rtnDTO.setPbCash(Optional.ofNullable(dbCashItem.getPbCash()).orElse(rtnDTO.getPbCash()));
        rtnDTO.setPbPoint(Optional.ofNullable(dbCashItem.getPbPoint()).orElse(rtnDTO.getPbPoint()));
        rtnDTO.setPbGift(Optional.ofNullable(dbCashItem.getPbGift()).orElse(rtnDTO.getPbGift()));
        rtnDTO.setPackageSn(Optional.ofNullable(dbCashItem.getPackageSn()).orElse(rtnDTO.getPackageSn()));
    }

    @Transactional
    public void batchChangeOnSale(CashShopBatchOnSaleReqDTO submit) {
        for (ModifiedCashItemDO data : submit.getData()) {
            switch (submit.getType()) {
                case "价格":
                    data.setPrice(submit.getValue());
                    break;
                case "数量":
                    data.setCount(submit.getValue().shortValue());
                    break;
                case "有效期":
                    data.setPeriod(submit.getValue().longValue());
                    break;
                case "优先级":
                    data.setPriority(submit.getValue());
                    break;
                case "上架状态":
                    data.setOnSale(submit.getValue());
                    break;
                case "性别":
                    data.setCommodityGender(submit.getValue());
                    break;
                case "标签":
                    data.setClz(submit.getValue());
                    break;
            }
            changeOnSale(data);
        }
    }

    public ItemInfoRtnDTO getItemInfoById(Integer itemId) {
        RequireUtil.requireNotNull(itemId, I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "itemId"));
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        String itemName = ii.getName(itemId);
        boolean valid = itemName != null;
        return ItemInfoRtnDTO.builder()
                .itemId(itemId)
                .itemName(itemName)
                .valid(valid)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public AddCashShopItemRtnDTO addCashShopItem(AddCashShopItemReqDTO req) {
        RequireUtil.requireNotNull(req.getItemId(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "itemId"));
        RequireUtil.requireNotNull(req.getCategoryId(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "categoryId"));
        RequireUtil.requireNotNull(req.getSubcategoryId(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "subcategoryId"));

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        String itemName = ii.getName(req.getItemId());
        if (itemName == null) {
            throw new BizException(I18nUtil.getExceptionMessage("UNKNOWN_PARAMETER_VALUE", "itemId", req.getItemId()));
        }

        getCategory(req.getCategoryId(), req.getSubcategoryId());

        // 检查是否已存在相同物品ID的商品，收集所有所在分类
        List<String> existingCategories = new ArrayList<>();
        for (ModifiedCashItemDO existingItem : CashShop.CashItemFactory.getItems().values()) {
            if (existingItem.getItemId() != null && existingItem.getItemId().equals(req.getItemId())) {
                String categoryInfo = getCategoryInfo(existingItem.getSn());
                if (!existingCategories.contains(categoryInfo)) {
                    existingCategories.add(categoryInfo);
                }
            }
        }
        for (ModifiedCashItemDO existingItem : CashShop.CashItemFactory.getModifiedCashItems().values()) {
            if (existingItem.getItemId() != null && existingItem.getItemId().equals(req.getItemId())) {
                String categoryInfo = getCategoryInfo(existingItem.getSn());
                if (!existingCategories.contains(categoryInfo)) {
                    existingCategories.add(categoryInfo);
                }
            }
        }

        // 如果物品已存在且不强制添加，返回已存在信息
        if (!existingCategories.isEmpty() && !Boolean.TRUE.equals(req.getForceAdd())) {
            return AddCashShopItemRtnDTO.builder()
                    .exists(true)
                    .itemId(req.getItemId())
                    .itemName(itemName)
                    .existingCategories(existingCategories)
                    .newSn(null)
                    .build();
        }

        String prefix = req.getCategoryId() + String.format("%02d", req.getSubcategoryId());
        
        int maxSn = 0;
        for (Integer sn : CashShop.CashItemFactory.getItems().keySet()) {
            if (String.valueOf(sn).startsWith(prefix) && sn > maxSn) {
                maxSn = sn;
            }
        }
        for (Integer sn : CashShop.CashItemFactory.getModifiedCashItems().keySet()) {
            if (String.valueOf(sn).startsWith(prefix) && sn > maxSn) {
                maxSn = sn;
            }
        }

        int newSn;
        if (maxSn == 0) {
            newSn = Integer.parseInt(prefix + "001");
        } else {
            newSn = maxSn + 1;
        }

        ModifiedCashItemDO cashItem = ModifiedCashItemDO.builder()
                .sn(newSn)
                .itemId(req.getItemId())
                .count(req.getCount() != null ? req.getCount() : (short) 1)
                .price(req.getPrice() != null ? req.getPrice() : 1000)
                .priority(req.getPriority() != null ? req.getPriority() : 0)
                .period(req.getPeriod() != null ? req.getPeriod() : 90L)
                .commodityGender(req.getCommodityGender() != null ? req.getCommodityGender() : 2)
                .onSale(req.getOnSale() != null ? req.getOnSale() : 1)
                .clz(req.getClz())
                .pbCash(req.getPbCash())
                .pbPoint(req.getPbPoint())
                .pbGift(req.getPbGift())
                .build();

        modifiedCashItemMapper.insertSelective(cashItem);
        CashShop.CashItemFactory.loadAllModifiedCashItems();

        return AddCashShopItemRtnDTO.builder()
                .exists(false)
                .itemId(req.getItemId())
                .itemName(itemName)
                .newSn(newSn)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCashShopItem(AddCashShopItemReqDTO req) {
        RequireUtil.requireNotNull(req.getSn(), I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "sn"));
        
        ModifiedCashItemDO wzItem = CashShop.CashItemFactory.getWzItem(req.getSn());
        ModifiedCashItemDO dbItem = CashShop.CashItemFactory.getModifiedCashItems().get(req.getSn());
        
        if (wzItem == null && dbItem == null) {
            throw new BizException(I18nUtil.getExceptionMessage("UNKNOWN_PARAMETER_VALUE", "sn", req.getSn()));
        }
        
        ModifiedCashItemDO baseItem = wzItem != null ? wzItem : dbItem;
        
        ModifiedCashItemDO updateData = ModifiedCashItemDO.builder()
                .sn(req.getSn())
                .build();
        
        if (req.getItemId() != null) {
            updateData.setItemId(req.getItemId());
        }
        if (req.getCount() != null) {
            updateData.setCount(req.getCount());
        }
        if (req.getPrice() != null) {
            updateData.setPrice(req.getPrice());
        }
        if (req.getPriority() != null) {
            updateData.setPriority(req.getPriority());
        }
        if (req.getPeriod() != null) {
            updateData.setPeriod(req.getPeriod());
        }
        if (req.getCommodityGender() != null) {
            updateData.setCommodityGender(req.getCommodityGender());
        }
        if (req.getOnSale() != null) {
            updateData.setOnSale(req.getOnSale());
        }
        if (req.getClz() != null) {
            updateData.setClz(req.getClz());
        }
        if (req.getPbCash() != null) {
            updateData.setPbCash(req.getPbCash());
        }
        if (req.getPbPoint() != null) {
            updateData.setPbPoint(req.getPbPoint());
        }
        if (req.getPbGift() != null) {
            updateData.setPbGift(req.getPbGift());
        }
        
        if (dbItem != null) {
            modifiedCashItemMapper.deleteById(req.getSn());
        }
        
        modifiedCashItemMapper.insertSelective(updateData);
        CashShop.CashItemFactory.loadAllModifiedCashItems();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCashShopItem(Integer sn) {
        RequireUtil.requireNotNull(sn, I18nUtil.getExceptionMessage("PARAMETER_SHOULD_NOT_NULL", "sn"));

        ModifiedCashItemDO wzItem = CashShop.CashItemFactory.getWzItem(sn);
        ModifiedCashItemDO dbItem = CashShop.CashItemFactory.getModifiedCashItems().get(sn);

        if (dbItem == null) {
            throw new BizException("该商品不存在于数据库中，无法删除");
        }

        if (wzItem == null) {
            // 纯新增商品：直接从数据库删除
            modifiedCashItemMapper.deleteById(sn);
        } else {
            // 修改自WZ的商品：还原为WZ原值（删除数据库记录）
            modifiedCashItemMapper.deleteById(sn);
        }

        CashShop.CashItemFactory.loadAllModifiedCashItems();
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteCashShopItem(List<Integer> sns) {
        for (Integer sn : sns) {
            deleteCashShopItem(sn);
        }
    }

    private String getCategoryInfo(int sn) {
        String snStr = String.valueOf(sn);
        try {
            // SN 格式: 第1位是一级分类ID，第2-3位是二级分类ID (如 10000000 -> 一级分类1, 二级分类00)
            if (snStr.length() >= 3) {
                int id = Integer.parseInt(snStr.substring(0, 1));
                int subId = Integer.parseInt(snStr.substring(1, 3));
                CashCategory category = getCategory(id, subId);
                return category.getName() + " - " + category.getSubName();
            }
            return "未知分类";
        } catch (Exception e) {
            return "未知分类";
        }
    }
}
