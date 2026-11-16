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
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * LivingEntity#onClimbable (LivingEntity.java:1735) asks BlockTags.CLIMBABLE, whose members include scaffolding,
 * glow berry crop (Blocks.CAVE_VINES) and cave vines. Baritone listed ladder and the four vine blocks by hand, so it
 * missed those and could not path up or down them.
 */
public class ClimbableTagTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void climbableFollowsTheTag() {
        bindClimbable(Blocks.SCAFFOLDING, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT);
        assertTrue("scaffolding is in BlockTags.CLIMBABLE", MovementHelper.isClimbable(Blocks.SCAFFOLDING));
        assertTrue("cave vines are in BlockTags.CLIMBABLE", MovementHelper.isClimbable(Blocks.CAVE_VINES));
        assertTrue("cave vine plants are in BlockTags.CLIMBABLE", MovementHelper.isClimbable(Blocks.CAVE_VINES_PLANT));
        assertFalse("oak planks are not in the tag", MovementHelper.isClimbable(Blocks.OAK_PLANKS));

        // A second, different tag proves the answer comes from the tag and not from another hardcoded list.
        bindClimbable(Blocks.OAK_PLANKS);
        assertTrue(MovementHelper.isClimbable(Blocks.OAK_PLANKS));
        assertFalse("ladder is not in this tag", MovementHelper.isClimbable(Blocks.LADDER));
    }

    @SuppressWarnings("unchecked")
    private static void bindClimbable(Block... blocks) {
        List<Holder<Block>> holders = java.util.Arrays.stream(blocks)
                .map(block -> (Holder<Block>) block.builtInRegistryHolder())
                .toList();
        Map<TagKey<Block>, List<Holder<Block>>> tags = Map.of(BlockTags.CLIMBABLE, holders);
        MappedRegistry<Block> registry = (MappedRegistry<Block>) BuiltInRegistries.BLOCK;
        registry.prepareTagReload(new TagLoader.LoadResult<>(Registries.BLOCK, tags)).apply();
    }
}
