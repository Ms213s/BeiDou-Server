package org.gms.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCashShopItemReqDTO {
    private Integer sn;
    private Integer itemId;
    private Integer categoryId;
    private Integer subcategoryId;
    private Short count;
    private Integer price;
    private Integer bonus;
    private Integer priority;
    private Long period;
    private Integer maplePoint;
    private Integer meso;
    private Integer forPremiumUser;
    private Integer commodityGender;
    private Integer onSale;
    private Integer clz;
    private Integer limit;
    private Integer pbCash;
    private Integer pbPoint;
    private Integer pbGift;
    private Integer packageSn;
    private Boolean forceAdd;
}
