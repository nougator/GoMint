/*
 * Copyright (c) 2018, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.inventory.item;

import io.gomint.inventory.item.ItemType;
import io.gomint.server.registry.RegisterInfo;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( sId = "minecraft:warped_button", id = -261 )
public class ItemWarpedButton extends ItemStack implements io.gomint.inventory.item.ItemWarpedButton {

    @Override
    public ItemType getItemType() {
        return ItemType.WARPED_BUTTON;
    }

}
