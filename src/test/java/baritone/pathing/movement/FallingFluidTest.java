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

import baritone.cache.ChunkPacker;
import baritone.utils.pathing.PathingBlockType;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Falling water (a waterfall) has amount 8 like a source block, so FluidState#isFull cannot tell the two apart.
 */
public class FallingFluidTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static BlockState sourceWater() {
        return Blocks.WATER.defaultBlockState();
    }

    private static BlockState fallingWater() {
        return Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock();
    }

    @Test
    public void sourceWaterIsStill() {
        assertFalse(MovementHelper.isFlowing(0, 0, 0, sourceWater(), null));
    }

    @Test
    public void sidewaysFlowingWaterIsFlowing() {
        assertTrue(MovementHelper.isFlowing(0, 0, 0, Fluids.FLOWING_WATER.getFlowing(7, false).createLegacyBlock(), null));
    }

    @Test
    public void fallingWaterIsFlowing() {
        assertTrue(MovementHelper.isFlowing(0, 0, 0, fallingWater(), null));
    }

    @Test
    public void fallingLavaIsFlowing() {
        assertTrue(MovementHelper.isFlowing(0, 0, 0, Fluids.FLOWING_LAVA.getFlowing(8, true).createLegacyBlock(), null));
    }

    @Test
    public void cacheStoresSourceWaterAsWater() throws ReflectiveOperationException {
        assertEquals(PathingBlockType.WATER, cachedType(sourceWater()));
    }

    @Test
    public void cacheAvoidsFallingWater() throws ReflectiveOperationException {
        assertEquals(PathingBlockType.AVOID, cachedType(fallingWater()));
    }

    private static PathingBlockType cachedType(BlockState state) throws ReflectiveOperationException {
        Method method = ChunkPacker.class.getDeclaredMethod("getPathingBlockType", BlockState.class, LevelChunk.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return (PathingBlockType) method.invoke(null, state, null, 0, 0, 0);
    }
}
