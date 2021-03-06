package io.gomint.server.world.block;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.world.block.BlockType;
import io.gomint.world.block.BlockWarpedFungus;

/**
 * @author KingAli
 * @version 1.0
 */
@RegisterInfo( sId = "minecraft:warped_fungus" )
public class WarpedFungus extends Block implements BlockWarpedFungus {

    @Override
    public String getBlockId() {
        return "minecraft:warped_fungus";
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public float getBlastResistance() {
        return 0;
    }

    @Override
    public long getBreakTime() {
        return 0;
    }

    @Override
    public BlockType getBlockType() {
        return BlockType.WARPED_FUNGUS;
    }

    @Override
    public boolean canBeBrokenWithHand() {
        return true;
    }
}
