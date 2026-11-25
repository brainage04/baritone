package baritone.utils;

import net.minecraft.world.item.ItemStack;

public final class MiningCostFixture {

    private MiningCostFixture() {
    }

    public static ToolSet create() {
        return new ToolSet(1.0, 0.2, block -> ToolSet.calculateSpeedVsBlock(ItemStack.EMPTY, block.defaultBlockState()));
    }
}
