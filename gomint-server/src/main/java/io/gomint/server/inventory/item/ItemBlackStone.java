package io.gomint.server.inventory.item;

import io.gomint.inventory.item.ItemType;
import io.gomint.server.registry.RegisterInfo;

/**
 * @author KingAli
 * @version 1.0
 */
@RegisterInfo(id = 528 ,sId = "minecraft:blackstone" )
public class ItemBlackStone extends ItemStack implements io.gomint.inventory.item.ItemBlackStone {

    @Override
    public String getBlockId() {
        return "minecraft:blackstone";
    }

    @Override
    public ItemType getType() {
        return ItemType.BLACKSTONE;
    }
}
