package io.gomint.server.inventory.item;

import io.gomint.inventory.item.ItemType;
import io.gomint.server.registry.RegisterInfo;

/**
 * @author KingAli
 * @version 1.0
 */
@RegisterInfo(sId = "minecraft:sweet_burry_bush", id = 462)
public class ItemSweetBerriesBush extends ItemStack implements io.gomint.inventory.item.ItemSweetBerriesBush {

    @Override
    public ItemType getItemType() {
        return ItemType.SWEETBERRIESBUSH;
    }

}
