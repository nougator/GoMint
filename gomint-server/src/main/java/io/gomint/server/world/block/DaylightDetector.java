package io.gomint.server.world.block;

import io.gomint.server.entity.tileentity.CommandBlockTileEntity;
import io.gomint.server.entity.tileentity.DaylightDetectorTileEntity;
import io.gomint.server.entity.tileentity.TileEntity;
import io.gomint.server.world.block.state.RedstoneSignalStrength;
import io.gomint.taglib.NBTTagCompound;
import io.gomint.world.block.BlockDaylightDetector;
import io.gomint.world.block.BlockType;

import io.gomint.server.registry.RegisterInfo;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( sId = "minecraft:daylight_detector" )
public class DaylightDetector extends Block implements BlockDaylightDetector {

    private static final RedstoneSignalStrength SIGNAL_STRENGTH = new RedstoneSignalStrength(() -> new String[]{"redstone_signal"});

    @Override
    public long getBreakTime() {
        return 300;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public float getBlastResistance() {
        return 1.0f;
    }

    @Override
    public BlockType getBlockType() {
        return BlockType.DAYLIGHT_DETECTOR;
    }

    @Override
    public boolean canBeBrokenWithHand() {
        return true;
    }

    @Override
    public boolean needsTileEntity() {
        return true;
    }

    @Override
    TileEntity createTileEntity( NBTTagCompound compound ) {
        super.createTileEntity( compound );
        return this.world.getServer().getTileEntities().construct(DaylightDetectorTileEntity.class, compound, this, this.world.getServer().getItems());
    }

}
