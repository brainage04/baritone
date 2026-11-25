/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.pathing.movement;

import baritone.api.Settings;
import baritone.api.TestSettings;
import baritone.pathing.precompute.PrecomputedData;
import baritone.utils.BlockStateInterface;
import baritone.utils.MiningCostFixture;
import baritone.utils.ToolSet;
import baritone.utils.pathing.BetterWorldBorder;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The game slows a player whose eye is in water to SUBMERGED_MINING_SPEED ({@code Player#getDestroySpeed},
 * Player.java:617-619 in 1.21.11). The eye is 1.62 above the feet, 0.62 up the block above the feet, so a block mined
 * from a position at the surface is mined with the eye out of the water even when the player started the search at the
 * bottom of the lake. The estimate asks this per stand position instead of once per search.
 */
public class MiningCostSubmergedTest {

    // the fluids are registry objects, so they can only be read once the game has bootstrapped
    private static FluidState AIR;
    private static Settings SETTINGS;
    private static FluidState SOURCE;

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        AIR = Fluids.EMPTY.defaultFluidState();
        SOURCE = Fluids.WATER.getSource(false);
        SETTINGS = TestSettings.create();
    }

    private static FluidState flowing(int amount) {
        return Fluids.FLOWING_WATER.getFlowing(amount, false);
    }

    @Test
    public void eyeAboveTheSurfaceIsNotSubmerged() {
        // a source block is 8/9 = 0.889 high and the eye is 0.62 up it, so a source with air above it covers the eye;
        // no fluid at the eye's block is a player standing out of the water
        assertTrue(MovementHelper.eyeInWater(SOURCE, AIR));
        assertFalse(MovementHelper.eyeInWater(AIR, AIR));
        assertFalse(MovementHelper.eyeInWater(AIR, SOURCE));
    }

    @Test
    public void shallowFlowingWaterStopsBelowTheEye() {
        // FlowingFluid#getOwnHeight is amount / 9: 5/9 = 0.556 is under the 0.62 eye, 6/9 = 0.667 is over it
        assertFalse(MovementHelper.eyeInWater(flowing(5), AIR));
        assertTrue(MovementHelper.eyeInWater(flowing(6), AIR));
    }

    @Test
    public void waterAboveTheEyeBlockFillsIt() {
        // a block with the same fluid above it is full height (FlowingFluid#getHeight), whatever its own amount
        assertTrue(MovementHelper.eyeInWater(flowing(1), SOURCE));
        assertTrue(MovementHelper.eyeInWater(flowing(1), flowing(1)));
    }

    @Test
    public void otherFluidsDoNotCount() {
        assertFalse(MovementHelper.eyeInWater(Fluids.LAVA.getSource(false), AIR));
        // the eye is in the block above the feet: the surface test compares against its height within that block
        float eye = MovementHelper.standingEyeHeight();
        assertTrue(eye > 1.0F && eye < 2.0F);
    }

    @Test
    public void swimmingIsNotStandingOnTheGround() {
        // canWalkOn calls water with water above it walkable (the swimmer moves through it), but a swimming player's
        // onGround is false, so the game's /5 airborne penalty applies to what it mines from there
        assertTrue(MovementHelper.isStandingSupport(Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState(), true, SETTINGS));
        assertFalse(MovementHelper.isStandingSupport(Blocks.WATER.defaultBlockState(), Blocks.WATER.defaultBlockState(), true, SETTINGS));
        assertFalse(MovementHelper.isStandingSupport(Blocks.AIR.defaultBlockState(), Blocks.WATER.defaultBlockState(), true, SETTINGS));
        assertFalse(MovementHelper.isStandingSupport(Blocks.AIR.defaultBlockState(), Blocks.LAVA.defaultBlockState(), true, SETTINGS));
        // a lily pad or carpet in the feet cell supplies its own collision shape, so the water under it is footing after all
        assertTrue(MovementHelper.isStandingSupport(Blocks.LILY_PAD.defaultBlockState(), Blocks.WATER.defaultBlockState(), true, SETTINGS));
        assertTrue(MovementHelper.isStandingSupport(Blocks.WHITE_CARPET.defaultBlockState(), Blocks.WATER.defaultBlockState(), true, SETTINGS));
        // ladders and vines have no collision (BlockBehaviour.java:322-324 in 26.3), so climbing is airborne even in vanilla
        assertFalse(MovementHelper.isStandingSupport(Blocks.LADDER.defaultBlockState(), Blocks.LADDER.defaultBlockState(), true, SETTINGS));
        assertFalse("a block that cannot be walked on is no support", MovementHelper.isStandingSupport(Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState(), false, SETTINGS));
    }

    @Test
    public void miningCostUsesTheSupportInTheFeetCell() {
        assertGroundedAboveWater(Blocks.LILY_PAD.defaultBlockState());
        assertGroundedAboveWater(Blocks.WHITE_CARPET.defaultBlockState());
        assertGroundedAboveWater(Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        assertGroundedAboveWater(Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
    }

    @Test
    public void swimmingClimbingAndBubbleColumnsHaveNoGroundSupport() {
        assertAirborneAboveWater(Blocks.WATER.defaultBlockState());
        assertAirborneAboveWater(Blocks.BUBBLE_COLUMN.defaultBlockState());
        assertAirborneAboveWater(Blocks.LADDER.defaultBlockState());
        assertAirborneAboveWater(Blocks.VINE.defaultBlockState());
    }

    @Test
    public void miningMemoSeparatesSupportedFeetFromSwimming() {
        // one world, one memo: the same target is mined once from a swimming stand and once from a lily pad stand
        // beside it, in both orders. The memo keys only the target, so if the airborne bit failed to separate the
        // tables the second query would read the first one's cost back.
        for (boolean swimmingFirst : new boolean[]{true, false}) {
            TestBlockStateInterface bsi = new TestBlockStateInterface();
            // the swimming stand at x=0: feet in deep water, water below
            bsi.set(0, FEET_Y - 1, Blocks.WATER.defaultBlockState());
            bsi.set(0, FEET_Y, Blocks.WATER.defaultBlockState());
            // the lily pad stand at x=2: pad in the feet cell over water, so the player stands on it
            bsi.set(2, FEET_Y - 1, Blocks.WATER.defaultBlockState());
            bsi.set(2, FEET_Y, Blocks.LILY_PAD.defaultBlockState());
            // the target at head height of both stands, within reach of neither distinction but the support
            bsi.set(1, TARGET_Y, Blocks.STONE.defaultBlockState());
            TestContext context = new TestContext(bsi, MiningCostFixture.create());
            context.claimSearchCaches();
            try {
                double first = miningCost(context, swimmingFirst ? 0 : 2);
                double second = miningCost(context, swimmingFirst ? 2 : 0);
                double swimCost = swimmingFirst ? first : second;
                double padCost = swimmingFirst ? second : first;
                TestContext ground = context(Blocks.STONE.defaultBlockState());
                ground.claimSearchCaches();
                double groundCost;
                try {
                    groundCost = miningCost(ground);
                } finally {
                    ground.releaseSearchCaches();
                }
                double penalty = context.breakBlockAdditionalCost;
                assertEquals("supported feet cost what dry ground costs", groundCost, padCost, 1.0E-9);
                assertEquals("a swimmer mines five times slower", (groundCost - penalty) * ToolSet.AIRBORNE_MINING_DIVISOR + penalty, swimCost, 1.0E-9);
            } finally {
                context.releaseSearchCaches();
            }
        }
    }

    private static void assertGroundedAboveWater(BlockState feet) {
        assertEquals(isolatedMiningCost(context(Blocks.STONE.defaultBlockState())), isolatedMiningCost(context(feet)), 1.0E-9);
    }

    private static void assertAirborneAboveWater(BlockState feet) {
        TestContext onGround = context(Blocks.STONE.defaultBlockState());
        double ground = isolatedMiningCost(onGround);
        double penalty = onGround.breakBlockAdditionalCost;
        assertEquals((ground - penalty) * ToolSet.AIRBORNE_MINING_DIVISOR + penalty, isolatedMiningCost(context(feet)), 1.0E-9);
    }


    private static double miningCost(TestContext context) {
        return miningCost(context, 0);
    }

    private static double miningCost(TestContext context, int standX) {
        return MovementHelper.getMiningDurationTicks(context, standX, FEET_Y, 0, 1, TARGET_Y, 0, false);
    }

    private static double isolatedMiningCost(TestContext context) {
        context.claimSearchCaches();
        try {
            return miningCost(context);
        } finally {
            context.releaseSearchCaches();
        }
    }

    private static TestContext context(BlockState feet) {
        TestBlockStateInterface bsi = new TestBlockStateInterface();
        bsi.set(0, FEET_Y - 1, Blocks.WATER.defaultBlockState());
        bsi.set(0, FEET_Y, feet);
        bsi.set(1, TARGET_Y, Blocks.STONE.defaultBlockState());
        ToolSet toolSet = MiningCostFixture.create();
        return new TestContext(bsi, toolSet);
    }

    private static final int FEET_Y = 1;
    private static final int TARGET_Y = 2;
    private static final int BLOCK_HEIGHT = 8;

    private static final class TestBlockStateInterface extends BlockStateInterface {
        private static final int WIDTH = 8;
        private final BlockState[] states = new BlockState[BLOCK_HEIGHT * WIDTH];

        private TestBlockStateInterface() {
            super(new BetterWorldBorder(new WorldBorder()), 0, BLOCK_HEIGHT);
            java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        }

        private void set(int x, int y, BlockState state) {
            states[y * WIDTH + x] = state;
        }

        @Override
        protected BlockState getUncached(int x, int y, int z) {
            if (x < 0 || x >= WIDTH || z != 0 || y < 0 || y >= BLOCK_HEIGHT) {
                return Blocks.AIR.defaultBlockState();
            }
            return states[y * WIDTH + x];
        }
    }

    private static final class TestContext extends CalculationContext {
        private TestContext(TestBlockStateInterface bsi, ToolSet toolSet) {
            super(null, false, null, null, bsi, toolSet, false, false, true, 0, 1.0F, new PrecomputedData(SETTINGS), SETTINGS);
        }
    }
 }
