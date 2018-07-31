package io.gomint.server.inventory.item;
import io.gomint.inventory.item.ItemType;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 178 )
 public class ItemInvertedDaylightSensor extends ItemStack implements io.gomint.inventory.item.ItemInvertedDaylightSensor {



    @Override
    public ItemType getType() {
        return ItemType.INVERTED_DAYLIGHT_SENSOR;
    }

}
