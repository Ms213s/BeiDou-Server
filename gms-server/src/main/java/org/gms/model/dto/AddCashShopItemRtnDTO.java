package org.gms.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCashShopItemRtnDTO {
    private boolean exists;
    private Integer itemId;
    private String itemName;
    private List<String> existingCategories;
    private Integer newSn;
}
