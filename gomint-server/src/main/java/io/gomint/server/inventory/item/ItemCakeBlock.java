package io.gomint.server.inventory.item;
import io.gomint.inventory.item.ItemType;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( sId = "minecraft:cake", id = 92 )
public class ItemCakeBlock extends ItemStack {

    @Override
    public ItemType getItemType() {
        return ItemType.CAKE_BLOCK;
    }

}
