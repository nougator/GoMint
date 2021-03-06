package io.gomint.server.inventory.item;

import io.gomint.inventory.item.ItemType;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo(sId = "minecraft:netherrack", id = 87)
public class ItemNetherrack extends ItemStack implements io.gomint.inventory.item.ItemNetherrack {

    @Override
    public ItemType getItemType() {
        return ItemType.NETHERRACK;
    }

}
