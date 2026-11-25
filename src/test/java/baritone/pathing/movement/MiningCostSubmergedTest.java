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

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
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
    private static FluidState SOURCE;

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        AIR = Fluids.EMPTY.defaultFluidState();
        SOURCE = Fluids.WATER.getSource(false);
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
        assertTrue(MovementHelper.isStandingSupport(Blocks.STONE.defaultBlockState(), true));
        assertFalse(MovementHelper.isStandingSupport(Blocks.WATER.defaultBlockState(), true));
        assertFalse(MovementHelper.isStandingSupport(Blocks.LAVA.defaultBlockState(), true));
        assertFalse("a block that cannot be walked on is no support", MovementHelper.isStandingSupport(Blocks.STONE.defaultBlockState(), false));
    }
}
