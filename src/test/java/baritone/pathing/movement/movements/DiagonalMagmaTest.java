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

package baritone.pathing.movement.movements;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A diagonal crosses two corner blocks. With allowWalkOnMagmaBlocks off, either corner being magma keeps the path
 * off the diagonal; MagmaBlock#stepOn damages a player walking over it (MagmaBlock.java:9-17).
 */
public class DiagonalMagmaTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static BlockState stone() {
        return Blocks.STONE.defaultBlockState();
    }

    private static BlockState magma() {
        return Blocks.MAGMA_BLOCK.defaultBlockState();
    }

    private static BlockState lava() {
        return Fluids.LAVA.defaultFluidState().createLegacyBlock();
    }

    @Test
    public void magmaOnTheFirstCornerBlocksTheDiagonal() {
        assertTrue(MovementDiagonal.crossesMagma(false, magma(), stone()));
    }

    @Test
    public void magmaOnTheSecondCornerBlocksTheDiagonal() {
        assertTrue(MovementDiagonal.crossesMagma(false, stone(), magma()));
    }

    @Test
    public void twoStoneCornersDoNotBlockTheDiagonal() {
        assertFalse(MovementDiagonal.crossesMagma(false, stone(), stone()));
    }

    @Test
    public void magmaIsAllowedWhenTheSettingAllowsIt() {
        assertFalse(MovementDiagonal.crossesMagma(true, magma(), magma()));
    }

    @Test
    public void lavaBlocksTheDiagonalEitherWay() {
        assertTrue(MovementDiagonal.crossesMagma(true, lava(), stone()));
        assertTrue(MovementDiagonal.crossesMagma(true, stone(), lava()));
    }
}
