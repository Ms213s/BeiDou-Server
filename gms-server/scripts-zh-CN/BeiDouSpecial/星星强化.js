var status = 0;
var selectedSlot;
var selectedItem;
var 星级; // 当前装备强化星级
var 强化石ID = 4310000; // 原先强化石ID（现不再使用）

var useProtect = false;
var TASK_FLAG = "STAR_PROTECT";
var PROTECT_COUNT_KEY = "STAR_PROTECT_COUNT";

// 标记是否处于确认是否继续强化阶段
var waitingConfirm = false;

function start() {
    status = -1;
    selectedSlot = null;
    selectedItem = null;
    waitingConfirm = false;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    // 在强化界面（状态>=1）取消对话（非确认阶段）直接退出
    if (status >= 1 && mode == 0 && !waitingConfirm) {
        cm.dispose();
        return;
    }
    
    // 处理"是否继续强化"确认阶段
    if (waitingConfirm) {
        if (mode != 1) {
            cm.dispose();
            return;
        } else {
            waitingConfirm = false;
            status = 3; // 跳转到状态3继续强化
        }
    } else {
        if (mode == -1) {
            cm.dispose();
            return;
        } else {
            if (mode == 1)
                status++;
            else
                status--;
        }
    }
    
    if (status == 0) {
        // 装备选择界面
        var text = "#b请选择你要强化的装备：#k\r\n";
        var inventory = cm.getInventory(1);
        var itemList = [];
        var ItemInfo = Packages.org.gms.server.ItemInformationProvider.getInstance();
        for (var i = 0; i < inventory.getSlotLimit(); i++) {
            var item = inventory.getItem(i);
            if (item != null && isValidEquipment(item)) {
                var owner = item.getOwner();
                var displayStar = (owner && owner.indexOf("★星之力★x") != -1) ? owner : "★星之力★x0";
                var itemName = ItemInfo.getName(item.getItemId());
                text += "#L" + i + "# #v" + item.getItemId() + "# " + itemName + " (" + displayStar + ") \r\n";
                itemList.push(i);
            }
        }
        if (itemList.length == 0) {
            cm.sendOk("你没有可强化的装备。\r\n注意：点装和装备类点装无法强化。");
            cm.dispose();
        } else {
            cm.sendSimple(text);
        }
    } else if (status == 1) {
        // 装备选定后，显示强化界面
        if (selectedSlot == null) {
            selectedSlot = selection;
            selectedItem = cm.getInventory(1).getItem(selectedSlot);
        }
        if (selectedItem == null || !isValidEquipment(selectedItem)) {
            cm.sendOk("无效的装备或该装备不能强化（可能是点装）。");
            cm.dispose();
            return;
        }
        // 读取装备原有的星级（若无则默认为0）
        星级 = getStarLevel(selectedItem.getOwner());
        var protectCount = getStarProtectCount();
        
        // 获取装备等级要求，用于计算金币消耗及最大星级限制
        var equipLevel = getEquipLevel(selectedItem);
        var maxStar = getMaxStar(equipLevel);
        if (星级 >= maxStar) {
            cm.sendOk("装备星级已达上限 (" + maxStar + "星)，无法强化。");
            cm.dispose();
            return;
        }
        
        // 金币消耗：装备等级*10000 + 星星等级*10000
        var goldCost = equipLevel * 10000 + 星级 * 10000;
        
        // 读取防爆状态（不再依赖日期）
        var extendData = cm.getCharacterExtendValue(TASK_FLAG) || "";
        useProtect = (extendData == "1");
        
        var ItemInfo = Packages.org.gms.server.ItemInformationProvider.getInstance();
        var itemName = ItemInfo.getName(selectedItem.getItemId());
        var text = "#b当前装备：#k" + itemName + "（★星之力★x" + 星级 + "）\r\n";
        text += "当前【星星防爆】剩余：" + protectCount + "\r\n\r\n";
        text += "【强化后果】\r\n";
        if (星级 < 10) {
            text += "  成功：星级 +1 (全属性 +1, HP/MP +10)\r\n";
            text += "  失败：星级 -1 (全属性 -1, HP/MP -10)\r\n";
        } else if (星级 == 10) {
            text += "  成功：星级 +1 (全属性 +3, HP/MP +15)\r\n";
            text += "  失败：星级 -1 (全属性 -1, HP/MP -10)\r\n";
        } else if (星级 < 20) { // 11～19星
            text += "  成功：星级 +1 (全属性 +3, HP/MP +15)\r\n";
            text += "  失败：星级 -1 (全属性 -3, HP/MP -15)\r\n";
        } else if (星级 == 20) {
            text += "  成功：星级 +1 (全属性 +5, HP/MP +20)\r\n";
            text += "  失败：星级 -1 (全属性 -3, HP/MP -15)\r\n";
        } else { // 21～(maxStar-1)
            text += "  成功：星级 +1 (全属性 +5, HP/MP +20)\r\n";
            text += "  失败：星级 -1 (全属性 -5, HP/MP -20)\r\n";
        }
        text += "  无变化：装备星级 -1，并获得1点【星星防爆】\r\n\r\n";
        text += "【概率说明】\r\n";
        var baseSuccess = getSuccessRate(星级);
        var baseFail = getFailRate(星级);
        var noChange = 100 - baseSuccess - baseFail;
        text += "  成功率： " + baseSuccess.toFixed(1) + "%\r\n";
        text += "  失败率： " + baseFail.toFixed(1) + "%\r\n";
        text += "  无变化率： " + noChange.toFixed(1) + "%\r\n";
        text += "金币消耗：" + goldCost + "\r\n\r\n";
        text += "#r请选择强化方式：#k\r\n";
        if (protectCount > 0) {
            if (useProtect) {
                text += "#L1# ■#r√星星防爆已开启#k ";
            } else {
                text += "#L1# □×星星防爆已关闭 ";
            }
        }
        text += "#L2# ◇确认强化\r\n";
        cm.sendSimple(text);
    } else if (status == 2) {
        // 处理防爆开关选择或执行强化操作
        if (selection == 1) {
            useProtect = !useProtect;
            cm.saveOrUpdateCharacterExtendValue(TASK_FLAG, (useProtect ? "1" : "0"));
            showEnhancementInterface();
        } else if (selection == 2) {
            // 检查金币是否足够
            var equipLevel = getEquipLevel(selectedItem);
            var goldCost = equipLevel * 10000 + 星级 * 10000;
            if (cm.getMeso() < goldCost) {
                cm.sendOk("你的金币不足，至少需要 " + goldCost + " 金币！");
                cm.dispose();
                return;
            }
            cm.gainMeso(-goldCost);
            
            // 计算属性变化幅度
            var attrAdd, hpmpAdd, failAttrAdd, failHpmpAdd;
            if (星级 < 10) {
                attrAdd = 1;
                hpmpAdd = 10;
                failAttrAdd = 1;
                failHpmpAdd = 10;
            } else if (星级 == 10) {
                attrAdd = 3;
                hpmpAdd = 15;
                failAttrAdd = 1;
                failHpmpAdd = 10;
            } else if (星级 < 20) { // 11~19星
                attrAdd = 3;
                hpmpAdd = 15;
                failAttrAdd = 3;
                failHpmpAdd = 15;
            } else if (星级 == 20) {
                attrAdd = 5;
                hpmpAdd = 20;
                failAttrAdd = 3;
                failHpmpAdd = 15;
            } else { // 21～(maxStar-1)
                attrAdd = 5;
                hpmpAdd = 20;
                failAttrAdd = 5;
                failHpmpAdd = 20;
            }
            
            // 概率判定
            var baseSuccess = getSuccessRate(星级);
            var baseFail = getFailRate(星级);
            var roll = Math.random() * 100;
            var resultMessage = "";
            if (roll < baseSuccess) {
                // 成功
                星级++;
                selectedItem.setOwner("★星之力★x" + 星级);
                applyStarEffects(selectedItem, attrAdd, hpmpAdd);
                resultMessage = "强化成功！当前星级：#r" + 星级 + "#k\r\n";
                resultMessage += "属性变化：全属性 +" + attrAdd + ", HP/MP +" + hpmpAdd;
                // 超过15星以上时全服公告
                if (星级 > 15) {
                    var Server = Packages.org.gms.net.server.Server;
                    var PacketCreator = Packages.org.gms.util.PacketCreator;
                    var message = "【强化公告】玩家 " + cm.getPlayer().getName() + " 的 " + 
                        Packages.org.gms.server.ItemInformationProvider.getInstance().getName(selectedItem.getItemId()) + 
                        " 强化成功，达到 ★" + 星级 + "！";
                    Server.getInstance().broadcastMessage(
                        cm.getClient().getWorld(),
                        PacketCreator.itemMegaphone(
                            message,
                            false,
                            cm.getClient().getChannel(),
                            selectedItem
                        )
                    );
                }
            } else if (roll < baseSuccess + baseFail) {
                // 失败
                if (useProtect && getStarProtectCount() > 0) {
                    setStarProtectCount(getStarProtectCount() - 1);
                    resultMessage = "强化失败，但【星星防爆】生效，不掉星！\r\n当前防爆剩余：#e" + getStarProtectCount() + "#k";
                } else {
                    applyStarEffects(selectedItem, -failAttrAdd, -failHpmpAdd);
                    星级--;
                    selectedItem.setOwner("★星之力★x" + 星级);
                    resultMessage = "强化失败，并且掉了一颗星！当前星级：#e" + 星级 + "#k\r\n";
                    resultMessage += "属性变化：全属性 -" + failAttrAdd + ", HP/MP -" + failHpmpAdd;
                }
            } else {
                // 无变化：装备星级降低1，并扣除属性，同时获得1点防爆
                applyStarEffects(selectedItem, -failAttrAdd, -failHpmpAdd);
                星级--;
                selectedItem.setOwner("★星之力★x" + 星级);
                setStarProtectCount(getStarProtectCount() + 1);
                resultMessage = "强化失败，装备星级降低1，但获得1点【星星防爆】！\r\n当前星级：#e" + 星级 + "#k\r\n";
                resultMessage += "属性变化：全属性 -" + failAttrAdd + ", HP/MP -" + failHpmpAdd + "\r\n";
                resultMessage += "当前防爆：" + getStarProtectCount();
            }
            cm.getPlayer().forceUpdateItem(selectedItem);
            cm.sendYesNo(resultMessage + "\r\n\r\n是否继续强化？");
            waitingConfirm = true;
        } else {
            cm.dispose();
        }
    } else if (status == 3) {
        // 状态3由确认阶段跳转而来，直接显示强化界面
        showEnhancementInterface();
    }
}

function showEnhancementInterface() {
    status = 1;
    if (selectedItem == null || !isValidEquipment(selectedItem)) {
        cm.sendOk("无效的装备或该装备不能强化（可能是点装）。");
        cm.dispose();
        return;
    }
    星级 = getStarLevel(selectedItem.getOwner());
    var equipLevel = getEquipLevel(selectedItem);
    var maxStar = getMaxStar(equipLevel);
    if (星级 >= maxStar) {
        cm.sendOk("装备星级已达上限 (" + maxStar + "星)，无法强化。");
        cm.dispose();
        return;
    }
    var protectCount = getStarProtectCount();
    var baseSuccess = getSuccessRate(星级);
    var baseFail = getFailRate(星级);
    var noChange = 100 - baseSuccess - baseFail;
    var goldCost = equipLevel * 10000 + 星级 * 10000;
    var extendData = cm.getCharacterExtendValue(TASK_FLAG) || "";
    useProtect = (extendData == "1");
    var ItemInfo = Packages.org.gms.server.ItemInformationProvider.getInstance();
    var itemName = ItemInfo.getName(selectedItem.getItemId());
    var text = "#b当前装备：#k" + itemName + "（★星之力★x" + 星级 + "）\r\n";
    text += "当前【星星防爆】剩余：" + protectCount + "\r\n\r\n";
    text += "【强化后果】\r\n";
    if (星级 < 10) {
        text += "  成功：星级 +1 (全属性 +1, HP/MP +10)\r\n";
        text += "  失败：星级 -1 (全属性 -1, HP/MP -10)\r\n";
    } else if (星级 == 10) {
        text += "  成功：星级 +1 (全属性 +3, HP/MP +15)\r\n";
        text += "  失败：星级 -1 (全属性 -1, HP/MP -10)\r\n";
    } else if (星级 < 20) { // 11～19星
        text += "  成功：星级 +1 (全属性 +3, HP/MP +15)\r\n";
        text += "  失败：星级 -1 (全属性 -3, HP/MP -15)\r\n";
    } else if (星级 == 20) {
        text += "  成功：星级 +1 (全属性 +5, HP/MP +20)\r\n";
        text += "  失败：星级 -1 (全属性 -3, HP/MP -15)\r\n";
    } else { // 21～(maxStar-1)
        text += "  成功：星级 +1 (全属性 +5, HP/MP +20)\r\n";
        text += "  失败：星级 -1 (全属性 -5, HP/MP -20)\r\n";
    }
    text += "  无变化：装备星级 -1，并获得1点【星星防爆】\r\n\r\n";
    text += "【概率说明】\r\n";
    text += "  成功率：" + baseSuccess.toFixed(1) + "%\r\n";
    text += "  失败率：" + baseFail.toFixed(1) + "%\r\n";
    text += "  无变化率：" + noChange.toFixed(1) + "%\r\n";
    text += "金币消耗：" + goldCost + "\r\n\r\n";
    text += "#r请选择强化方式：#k\r\n";
    if (protectCount > 0) {
        if (useProtect) {
            text += "#L1# ■#r√星星防爆已开启#k ";
        } else {
            text += "#L1# □×星星防爆已关闭 ";
        }
    }
    text += "#L2# ◇确认强化\r\n";
    cm.sendSimple(text);
}

function isValidEquipment(item) {
    var itemId = item.getItemId();
    if (itemId < 1000000 || itemId >= 2000000) {
        return false;
    }
    var ItemInfo = Packages.org.gms.server.ItemInformationProvider.getInstance();
    return !ItemInfo.isCash(itemId);
}

function getStarLevel(owner) {
    if (owner == null || owner === "") {
        return 0;
    }
    var match = owner.match(/★星之力★x(\d+)/);
    return match ? parseInt(match[1]) : 0;
}

/**
 * 读取装备等级要求。调用服务器端 getEquipStats(itemId) 返回的 Map，
 * 从中取出 "reqLevel" 字段。
 */
function getEquipLevel(item) {
    var ii = Packages.org.gms.server.ItemInformationProvider.getInstance();
    var stats = ii.getEquipStats(item.getItemId());
    if (stats == null) {
        return 0;
    }
    return stats.get("reqLevel");
}

/**
 * 根据装备等级要求返回强化上限星级
 */
function getMaxStar(equipLevel) {
    if (equipLevel <= 30) {
        return 5;
    } else if (equipLevel <= 70) {
        return 8;
    } else if (equipLevel <= 120) {
        return 16;
    } else if (equipLevel <= 160) {
        return 24;
    } else if (equipLevel <= 200) {
        return 30;
    } else {
        return 30;
    }
}

function applyStarEffects(item, deltaAttr, deltaHPMP) {
    item.setStr(item.getStr() + deltaAttr);
    item.setDex(item.getDex() + deltaAttr);
    item.setInt(item.getInt() + deltaAttr);
    item.setLuk(item.getLuk() + deltaAttr);
    item.setHp(item.getHp() + deltaHPMP);
    item.setMp(item.getMp() + deltaHPMP);
    item.setWatk(item.getWatk() + deltaAttr);
    item.setMatk(item.getMatk() + deltaAttr);
    item.setWdef(item.getWdef() + deltaAttr);
    item.setMdef(item.getMdef() + deltaAttr);
    item.setAcc(item.getAcc() + deltaAttr);
    item.setAvoid(item.getAvoid() + deltaAttr);
    item.setHands(item.getHands() + deltaAttr);
    item.setSpeed(item.getSpeed() + deltaAttr);
    item.setJump(item.getJump() + deltaAttr);
    cm.getPlayer().forceUpdateItem(item);
}

function getSuccessRate(s) {
    if (s < 9) {
        return 100 - (40 / 9) * s;
    } else if (s < 19) {
        return 60 - 4 * (s - 9);
    } else if (s < 29) {
        return 20 - 1.9 * (s - 19);
    } else {
        return 1;
    }
}

function getFailRate(s) {
    if (s < 9) {
        return (30 / 9) * s;
    } else if (s < 19) {
        return 30 + 3 * (s - 9);
    } else if (s < 29) {
        return 60 + 2 * (s - 19);
    } else {
        return 80;
    }
}

function getTodayString() {
    var today = new Date();
    return today.getFullYear() + "-" +
           (today.getMonth() + 1 < 10 ? "0" : "") + (today.getMonth() + 1) + "-" +
           (today.getDate() < 10 ? "0" : "") + today.getDate();
}

function getStarProtectCount() {
    var data = cm.getCharacterExtendValue(PROTECT_COUNT_KEY) || "0";
    return parseInt(data);
}

function setStarProtectCount(count) {
    cm.saveOrUpdateCharacterExtendValue(PROTECT_COUNT_KEY, count.toString());
}