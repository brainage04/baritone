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
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The elytra landing search accepted a hardcoded list of blocks, so it found no landing spot over basalt, blackstone
 * or tuff. The overworld floor is {@code BASE_STONE_OVERWORLD} (stone, granite, diorite, andesite, tuff, deepslate) and
 * the Nether floor is {@code BASE_STONE_NETHER} (netherrack, basalt, blackstone); the list named stone, deepslate and
 * netherrack and left the other six out.
 */
public class ElytraLandingBlockTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // The tags are data, not code: Bootstrap registers the blocks but binds no tags, so the vanilla data pack has
        // to be read before BlockState#is can answer.
        try (MultiPackResourceManager packs = new MultiPackResourceManager(PackType.SERVER_DATA,
                java.util.List.of(ServerPacksSource.createVanillaPackSource()))) {
            for (Registry.PendingTags<?> pending : TagLoader.loadTagsForExistingRegistries(packs, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY))) {
                pending.apply();
            }
        }
    }

    @Test
    public void acceptsEveryBaseStone() {
        for (Block block : new Block[]{Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.TUFF,
                Blocks.DEEPSLATE, Blocks.NETHERRACK, Blocks.BASALT, Blocks.BLACKSTONE}) {
            assertTrue(block + " is in a base stone tag and must be landable", ElytraProcess.isSafeBlock(block, false));
        }
    }

    @Test
    public void theTagsHoldWhatTheListMissed() {
        assertTrue(Blocks.BASALT.defaultBlockState().is(BlockTags.BASE_STONE_NETHER));
        assertTrue(Blocks.BLACKSTONE.defaultBlockState().is(BlockTags.BASE_STONE_NETHER));
        assertTrue(Blocks.TUFF.defaultBlockState().is(BlockTags.BASE_STONE_OVERWORLD));
        assertTrue(Blocks.ANDESITE.defaultBlockState().is(BlockTags.BASE_STONE_OVERWORLD));
        assertTrue(Blocks.DIORITE.defaultBlockState().is(BlockTags.BASE_STONE_OVERWORLD));
        assertTrue(Blocks.GRANITE.defaultBlockState().is(BlockTags.BASE_STONE_OVERWORLD));
    }

    @Test
    public void acceptsTheBlocksAPlayerStandsOn() {
        for (Block block : new Block[]{Blocks.SNOW, Blocks.ICE, Blocks.SAND, Blocks.RED_SAND,
                Blocks.GRASS_BLOCK, Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.COBBLESTONE, Blocks.GRAVEL,
                Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.TERRACOTTA, Blocks.MYCELIUM, Blocks.PODZOL,
                Blocks.DARK_OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.END_STONE}) {
            assertTrue(block + " must be landable", ElytraProcess.isSafeBlock(block, false));
        }
    }

    @Test
    public void rejectsWhatCannotBeStoodOn() {
        for (Block block : new Block[]{Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.MAGMA_BLOCK, Blocks.COBWEB,
                Blocks.POWDER_SNOW, Blocks.CACTUS, Blocks.SWEET_BERRY_BUSH, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
                Blocks.HOPPER, Blocks.CAULDRON, Blocks.OAK_FENCE, Blocks.LADDER, Blocks.PURPUR_BLOCK,
                Blocks.END_STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE}) {
            assertFalse(block + " must not be landable", ElytraProcess.isSafeBlock(block, false));
        }
    }

    @Test
    public void netherBricksFollowTheSetting() {
        assertFalse("nether bricks are off by default", ElytraProcess.isSafeBlock(Blocks.NETHER_BRICKS, false));
        assertTrue("nether bricks are landable when the setting is on", ElytraProcess.isSafeBlock(Blocks.NETHER_BRICKS, true));
    }
}
