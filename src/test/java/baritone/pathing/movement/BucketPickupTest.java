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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * BucketItem#use (BucketItem.java:60-80) fills an empty bucket from any BucketPickup block whose pickupBlock returns
 * a stack. LiquidBlock#pickupBlock (LiquidBlock.java:203-210) returns one only when LEVEL is 0, so a waterfall fills
 * nothing. MovementFall used to select the bucket for any water, including a waterfall, and then hold right click
 * forever without picking anything up.
 */
public class BucketPickupTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void sourceWaterFillsABucket() {
        assertTrue(MovementHelper.canPickUpWater(Blocks.WATER.defaultBlockState()));
    }

    @Test
    public void flowingWaterDoesNotFillABucket() {
        BlockState flowing = Fluids.FLOWING_WATER.getFlowing(7, false).createLegacyBlock();
        assertFalse(MovementHelper.canPickUpWater(flowing));
    }

    @Test
    public void fallingWaterDoesNotFillABucket() {
        BlockState falling = Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock();
        assertFalse(MovementHelper.canPickUpWater(falling));
    }

    /**
     * SimpleWaterloggedBlock#pickupBlock (SimpleWaterloggedBlock.java:39-49) replaces a waterlogged block with the
     * dry one and hands back a water bucket, so a waterlogged stair and slab do fill an empty bucket.
     */
    @Test
    public void waterloggedStairFillsABucket() {
        BlockState waterlogged = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        assertTrue(MovementHelper.canPickUpWater(waterlogged));
    }

    @Test
    public void waterloggedSlabFillsABucket() {
        BlockState waterlogged = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        assertTrue(MovementHelper.canPickUpWater(waterlogged));
    }

    @Test
    public void dryWaterloggedBlockDoesNotFillABucket() {
        BlockState dry = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
        assertFalse(MovementHelper.canPickUpWater(dry));
    }

    /**
     * BubbleColumnBlock#pickupBlock returns a water bucket unconditionally.
     */
    @Test
    public void bubbleColumnFillsABucket() {
        assertTrue(MovementHelper.canPickUpWater(Blocks.BUBBLE_COLUMN.defaultBlockState()));
    }

    /**
     * Seagrass and kelp hold a water source but are not BucketPickup, so BucketItem#use fails on them. This is the case
     * the instanceof check exists for.
     */
    @Test
    public void seagrassAndKelpDoNotFillABucket() {
        assertFalse(MovementHelper.canPickUpWater(Blocks.SEAGRASS.defaultBlockState()));
        assertFalse(MovementHelper.canPickUpWater(Blocks.KELP.defaultBlockState()));
        assertFalse(MovementHelper.canPickUpWater(Blocks.KELP_PLANT.defaultBlockState()));
    }

    @Test
    public void lavaDoesNotFillABucket() {
        assertFalse(MovementHelper.canPickUpWater(Blocks.LAVA.defaultBlockState()));
        assertFalse(MovementHelper.canPickUpWater(Blocks.LAVA_CAULDRON.defaultBlockState()));
    }
}
