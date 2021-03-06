package io.gomint.server.world.block;

import io.gomint.inventory.item.ItemLeaves;
import io.gomint.inventory.item.ItemShears;
import io.gomint.inventory.item.ItemStack;
import io.gomint.server.registry.RegisterInfo;
import io.gomint.server.world.block.state.BooleanBlockState;
import io.gomint.server.world.block.state.EnumBlockState;
import io.gomint.world.block.BlockLeaves;
import io.gomint.world.block.BlockType;
import io.gomint.world.block.data.LogType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo(sId = "minecraft:leaves", def = true)
@RegisterInfo(sId = "minecraft:leaves2")
public class Leaves extends Block implements BlockLeaves {

    private static final String OLD_LOG_TYPE = "old_leaf_type";
    private static final String OLD_LOG_ID = "minecraft:leaves";

    private static final String NEW_LOG_TYPE = "new_leaf_type";
    private static final String NEW_LOG_ID = "minecraft:leaves2";

    private enum LeaveTypeMagic {
        OAK(OLD_LOG_ID, OLD_LOG_TYPE, "oak"),
        SPRUCE(OLD_LOG_ID, OLD_LOG_TYPE, "spruce"),
        BIRCH(OLD_LOG_ID, OLD_LOG_TYPE, "birch"),
        JUNGLE(OLD_LOG_ID, OLD_LOG_TYPE, "jungle"),
        ACACIA(NEW_LOG_ID, NEW_LOG_TYPE, "acacia"),
        DARK_OAK(NEW_LOG_ID, NEW_LOG_TYPE, "dark_oak");

        private final String key;
        private final String value;
        private final String blockId;

        LeaveTypeMagic(String blockId, String key, String value) {
            this.key = key;
            this.value = value;
            this.blockId = blockId;
        }
    }

    private static final EnumBlockState<LeaveTypeMagic, String> VARIANT = new EnumBlockState<>(v -> {
        if (v == null) {
            return new String[]{OLD_LOG_TYPE, NEW_LOG_TYPE};
        }

        for (LeaveTypeMagic value : LeaveTypeMagic.values()) {
            if (value.value.equals(v)) {
                return value.key.equals(OLD_LOG_TYPE) ? new String[]{OLD_LOG_TYPE, NEW_LOG_TYPE} : new String[]{NEW_LOG_TYPE, OLD_LOG_TYPE};
            }
        }

        return new String[]{OLD_LOG_TYPE, NEW_LOG_TYPE};
    }, LeaveTypeMagic.values(), v -> v.value, v -> {
        for (LeaveTypeMagic value : LeaveTypeMagic.values()) {
            if (value.value.equals(v)) {
                return value;
            }
        }

        return null;
    });

    private static final BooleanBlockState UPDATE_FOR_DECAY = new BooleanBlockState( () -> new String[]{"update_bit"});
    private static final BooleanBlockState PERSISTENT = new BooleanBlockState( () -> new String[]{"persistent_bit"});

    @Override
    public long getBreakTime() {
        return 300;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public boolean canBeBrokenWithHand() {
        return true;
    }

    @Override
    public float getBlastResistance() {
        return 1.0f;
    }

    @Override
    public BlockType getBlockType() {
        return BlockType.LEAVES;
    }

    @Override
    public List<ItemStack> getDrops(ItemStack itemInHand) {
        return new ArrayList<>() {{
            if (isCorrectTool(itemInHand)) {
                add(ItemLeaves.create(1));
            }
        }};
    }

    @Override
    public Class<? extends ItemStack>[] getToolInterfaces() {
        return new Class[]{
            ItemShears.class
        };
    }

    @Override
    public void setLeaveType(LogType type) {
        LeaveTypeMagic newState = LeaveTypeMagic.valueOf(type.name());
        this.setBlockIdOnStateChange(newState.blockId); // We ignore the two keys here to get a known wrong value, the set state below will select the correct runtime id
        VARIANT.setState(this, newState);
    }

    @Override
    public LogType getLeaveType() {
        return LogType.valueOf(VARIANT.getState(this).name());
    }

}
