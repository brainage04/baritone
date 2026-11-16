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
import net.minecraft.core.HolderSet;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
        Map<TagKey<Block>, List<Holder<Block>>> before = snapshotTags();
        // A second, unrelated tag rides along so the test can see what a rebind does to the tags it does not name.
        bindTags(Map.of(
                BlockTags.CLIMBABLE, holders(Blocks.SCAFFOLDING, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT),
                BlockTags.LOGS, holders(Blocks.OAK_LOG)));
        assertTrue("the second tag is bound", Blocks.OAK_LOG.defaultBlockState().is(BlockTags.LOGS));
        assertTrue("scaffolding is in BlockTags.CLIMBABLE", MovementHelper.isClimbable(Blocks.SCAFFOLDING));
        assertTrue("cave vines are in BlockTags.CLIMBABLE", MovementHelper.isClimbable(Blocks.CAVE_VINES));
        assertTrue("cave vine plants are in BlockTags.CLIMBABLE", MovementHelper.isClimbable(Blocks.CAVE_VINES_PLANT));
        assertFalse("oak planks are not in the tag", MovementHelper.isClimbable(Blocks.OAK_PLANKS));

        // Different members prove the answer comes from the tag and not from another hardcoded list.
        bindClimbable(Blocks.OAK_PLANKS);
        assertTrue(MovementHelper.isClimbable(Blocks.OAK_PLANKS));
        assertFalse("ladder is not in this tag", MovementHelper.isClimbable(Blocks.LADDER));
        assertFalse("apply() replaced the whole tag set, so the tag it did not name is gone",
                Blocks.OAK_LOG.defaultBlockState().is(BlockTags.LOGS));

        bindTags(before);
        assertFalse("the restored tag does not hold oak planks", MovementHelper.isClimbable(Blocks.OAK_PLANKS));

        // apply() hands the registry the map it was given and nothing else (MappedRegistry.java:445), and rebinds every
        // holder's tag list from that map (:341-351). The registry is shared by every test in the JVM, so a binding
        // that names one tag deletes every other tag for the tests that run after this one.
        assertEquals("every block tag the JVM started with must still be bound", before, snapshotTags());
    }

    private static Map<TagKey<Block>, List<Holder<Block>>> snapshotTags() {
        Map<TagKey<Block>, List<Holder<Block>>> snapshot = new LinkedHashMap<>();
        for (HolderSet.Named<Block> tag : ((MappedRegistry<Block>) BuiltInRegistries.BLOCK).getTags().toList()) {
            List<Holder<Block>> members = new ArrayList<>(tag.size());
            for (Holder<Block> holder : tag) {
                members.add(holder);
            }
            snapshot.put(tag.key(), members);
        }
        return snapshot;
    }

    private static void bindClimbable(Block... blocks) {
        bindTags(Map.of(BlockTags.CLIMBABLE, holders(blocks)));
    }

    @SuppressWarnings("unchecked")
    private static List<Holder<Block>> holders(Block... blocks) {
        return java.util.Arrays.stream(blocks)
                .map(block -> (Holder<Block>) block.builtInRegistryHolder())
                .toList();
    }

    private static void bindTags(Map<TagKey<Block>, List<Holder<Block>>> tags) {
        ((MappedRegistry<Block>) BuiltInRegistries.BLOCK)
                .prepareTagReload(new TagLoader.LoadResult<>(Registries.BLOCK, tags)).apply();
    }
}
