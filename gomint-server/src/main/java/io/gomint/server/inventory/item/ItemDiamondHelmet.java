package io.gomint.server.inventory.item;

import io.gomint.inventory.item.ItemType;
import io.gomint.math.Vector;
import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.registry.RegisterInfo;
import io.gomint.world.block.Block;
import io.gomint.world.block.data.Facing;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 310 )
public class ItemDiamondHelmet extends ItemDiamondArmor implements io.gomint.inventory.item.ItemDiamondHelmet {

    @Override
    public float getReductionValue() {
        return 3;
    }

    @Override
    public boolean interact(EntityPlayer entity, Facing face, Vector clickPosition, Block clickedBlock ) {
        if ( clickedBlock == null ) {
            if ( isBetter( (ItemStack) entity.getArmorInventory().getHelmet() ) ) {
                ItemStack old = (ItemStack) entity.getArmorInventory().getHelmet();
                entity.getArmorInventory().setHelmet( this );
                entity.getInventory().setItem( entity.getInventory().getItemInHandSlot(), old );
            }
        }

        return false;
    }

    @Override
    public ItemType getItemType() {
        return ItemType.DIAMOND_HELMET;
    }

}
