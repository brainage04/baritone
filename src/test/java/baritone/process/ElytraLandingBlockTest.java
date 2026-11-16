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

package baritone.process;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The elytra landing search used to accept a hardcoded list of blocks, so it found no landing spot over 26.3's newer
 * stone: the overworld floor is {@code BlockTags.BASE_STONE_OVERWORLD} (andesite, diorite, granite, tuff, calcite,
 * deepslate) and the Nether floor is {@code BASE_STONE_NETHER} (netherrack, basalt, blackstone), and basalt, blackstone
 * and tuff were not in the list.
 */
public class ElytraLandingBlockTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void acceptsEveryBaseStone() {
        assertLandable(Blocks.BASALT);
        assertLandable(Blocks.BLACKSTONE);
        assertLandable(Blocks.TUFF);
        assertLandable(Blocks.ANDESITE);
        assertLandable(Blocks.DIORITE);
        assertLandable(Blocks.GRANITE);
        assertLandable(Blocks.CALCITE);
        assertLandable(Blocks.DEEPSLATE);
        assertLandable(Blocks.NETHERRACK);
        assertLandable(Blocks.STONE);
        assertLandable(Blocks.END_STONE);
        assertLandable(Blocks.SOUL_SAND);
    }

    @Test
    public void rejectsWhatCannotBeStoodOn() {
        assertNotLandable(Blocks.AIR);
        assertNotLandable(Blocks.WATER);
        assertNotLandable(Blocks.LAVA);
        assertNotLandable(Blocks.CACTUS);
        assertNotLandable(Blocks.MAGMA_BLOCK);
        assertNotLandable(Blocks.COBWEB);
    }

    @Test
    public void netherBricksFollowTheSetting() {
        assertFalse("nether bricks are off by default", ElytraProcess.isSafeLandingBlock(level(Blocks.NETHER_BRICKS), BlockPos.ZERO, false));
        assertTrue(ElytraProcess.isSafeLandingBlock(level(Blocks.NETHER_BRICKS), BlockPos.ZERO, true));
    }

    private static void assertLandable(net.minecraft.world.level.block.Block block) {
        assertTrue(block + " is a base stone and must be landable",
                ElytraProcess.isSafeLandingBlock(level(block), BlockPos.ZERO, false));
    }

    private static void assertNotLandable(net.minecraft.world.level.block.Block block) {
        assertFalse(block + " must not be landable",
                ElytraProcess.isSafeLandingBlock(level(block), BlockPos.ZERO, false));
    }

    private static BlockGetter level(net.minecraft.world.level.block.Block block) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        states.put(BlockPos.ZERO, block.defaultBlockState());
        return new BlockGetter() {
            @Override
            public BlockEntity getBlockEntity(BlockPos pos) {
                return null;
            }

            @Override
            public BlockState getBlockState(BlockPos pos) {
                return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
            }

            @Override
            public FluidState getFluidState(BlockPos pos) {
                return getBlockState(pos).getFluidState();
            }

            @Override
            public int getMinY() {
                return 0;
            }

            @Override
            public int getHeight() {
                return 256;
            }
        };
    }
}
