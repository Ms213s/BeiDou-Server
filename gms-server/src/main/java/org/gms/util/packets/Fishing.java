package org.gms.util.packets;

import org.gms.client.Character;
import org.gms.constants.id.ItemId;
import org.gms.constants.id.MapId;
import org.gms.util.NumberTool;
import org.gms.server.ItemInformationProvider;
import org.gms.util.PacketCreator;

/**
 * @author FateJiki (RaGeZONE)
 * @author Ronan - timing pattern
 * @author 重构钓鱼系统 - 快乐百宝券版本
 */
public class Fishing {

    public static void doFishing(Character chr, int ticketType) {
        if (!chr.isLoggedInWorld() || !chr.isAlive()) {
            return;
        }

        if (!MapId.isFishingArea(chr.getMapId())) {
            chr.dropMessage("你不在钓鱼区域！");
            return;
        }

        if (chr.getLevel() < 30) {
            chr.dropMessage(5, "你必须30级以上才能钓鱼！");
            return;
        }

        String ticketName;
        double successRate;
        
        if (ticketType == ItemId.HAPPY_TICKET) {
            ticketName = "快乐百宝券";
            successRate = 0.10 + 0.10;
        } else if (ticketType == ItemId.ADVANCED_HAPPY_TICKET) {
            ticketName = "高级快乐百宝券";
            successRate = 0.10 + 0.20;
        } else {
            return;
        }

        successRate *= chr.getWorldServer().getFishingRate();

        String fishingEffect;
        if (Math.random() > successRate) {
            fishingEffect = "Effect/BasicEff.img/Catch/Fail";
            chr.dropMessage(5, "消耗了" + ticketName + "，钓鱼失败！");
        } else {
            String rewardStr = "";
            fishingEffect = "Effect/BasicEff.img/Catch/Success";

            int rand = (int) (3.0 * Math.random());
            switch (rand) {
                case 0:
                    int mesoAward = NumberTool.doubleToInt((1400.0 * Math.random() + 1201.0) * chr.getMesoRate()) + (15 * chr.getLevel() / 5);
                    chr.gainMeso(mesoAward, true, true, true);

                    rewardStr = mesoAward + " 金币";
                    break;
                case 1:
                    int expAward = NumberTool.doubleToInt((645.0 * Math.random() + 620.0) * chr.getExpRate()) + (15 * chr.getLevel() / 4);
                    chr.gainExp(expAward, true, true);

                    rewardStr = expAward + " 经验";
                    break;
                case 2:
                    int itemid = getRandomItem();
                    rewardStr = ItemInformationProvider.getInstance().getName(itemid);

                    if (chr.canHold(itemid)) {
                        chr.getAbstractPlayerInteraction().gainItem(itemid, true);
                    } else {
                        chr.showHint("背包已满，无法获得 #r" + ItemInformationProvider.getInstance().getName(itemid) + "#k！");
                        rewardStr += " (背包已满)";
                    }
                    break;
            }

            chr.getMap().dropMessage(6, chr.getName() + " 消耗了" + ticketName + "，钓到了 " + rewardStr + "！");
        }

        chr.sendPacket(PacketCreator.showInfo(fishingEffect));
        chr.getMap().broadcastMessage(chr, PacketCreator.showForeignInfo(chr.getId(), fishingEffect), false);
    }

    public static int getRandomItem() {
        int rand = (int) (100.0 * Math.random());
        int[] commons = {1002851, 2002020, 2002020, ItemId.MANA_ELIXIR, 2000018, 2002018, 2002024, 2002027, 2002027, 2000018, 2000018, 2000018, 2000018, 2002030, 2002018, 2000016};
        int[] uncommons = {1000025, 1002662, 1002812, 1002850, 1002881, 1002880, 1012072, 4020009, 2043220, 2043022, 2040543, 2044420, 2040943, 2043713, 2044220, 2044120, 2040429, 2043220, 2040943};
        int[] rares = {1002859, 1002553, 1002762, 1002763, 1002764, 1002765, 1002766, 1002663, 1002788, 1002949, 2049100, 2340000, 2040822, 2040822, 2040822, 2040822};

        if (rand >= 25) {
            return commons[(int) (commons.length * Math.random())];
        } else if (rand <= 7 && rand >= 4) {
            return uncommons[(int) (uncommons.length * Math.random())];
        } else {
            return rares[(int) (rares.length * Math.random())];
        }
    }
}
