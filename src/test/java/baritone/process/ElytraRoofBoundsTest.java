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

import baritone.api.utils.BetterBlockPos;
import baritone.process.elytra.ElytraBehavior;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.OptionalLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The elytra bounds used the literal 127 for the Nether roof and gated it on {@code Level.NETHER}, so a datapack
 * dimension with {@code has_ceiling} and a different {@code logical_height} got the wrong ceiling. The game decides
 * the roof by data: the Nether's dimension type is minY 0, height 256, logicalHeight 128, hasCeiling true, and
 * vanilla uses {@code minY + logicalHeight - 1} for it (PortalForcer.java:59, ServerLevel#getLogicalHeight).
 */
public class ElytraRoofBoundsTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static DimensionType dimensionType(boolean hasCeiling, int minY, int height, int logicalHeight) {
        return new DimensionType(
                OptionalLong.empty(), true, hasCeiling, false, true, 1.0, true, false,
                minY, height, logicalHeight, BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F,
                new DimensionType.MonsterSettings(false, true, ConstantInt.of(0), 0));
    }

    @Test
    public void vanillaNetherRoofIs127() {
        DimensionType nether = dimensionType(true, 0, 256, 128);
        assertEquals(127, ElytraProcess.roofY(nether));
        assertEquals("a destination on the other side of the roof is moved to the air above it, not into the bedrock",
                128, ElytraProcess.aboveRoofY(nether));
        assertTrue(ElytraProcess.isInBounds(nether, new BlockPos(0, 126, 0), false));
        assertFalse("the roof itself is out of bounds", ElytraProcess.isInBounds(nether, new BlockPos(0, 127, 0), false));
        assertTrue("elytraAllowAboveRoof lifts the ceiling", ElytraProcess.isInBounds(nether, new BlockPos(0, 127, 0), true));
    }

    @Test
    public void datapackCeilingFollowsItsOwnLogicalHeight() {
        DimensionType tall = dimensionType(true, 0, 256, 192);
        assertEquals(191, ElytraProcess.roofY(tall));
        assertTrue(ElytraProcess.isInBounds(tall, new BlockPos(0, 190, 0), false));
        assertFalse("a ceiling at 191 must not be treated as the Nether's 127",
                ElytraProcess.isInBounds(tall, new BlockPos(0, 191, 0), false));
    }

    @Test
    public void dimensionWithoutCeilingIsNotCappedAtTheNetherRoof() {
        DimensionType overworld = dimensionType(false, -64, 384, 384);
        assertTrue(ElytraProcess.isInBounds(overworld, new BlockPos(0, 200, 0), false));
        assertFalse(ElytraProcess.isInBounds(overworld, new BlockPos(0, -65, 0), false));
    }

    /**
     * With elytraAllowAboveRoof on, a goal above the roof is accepted, and the flight moves it to the room's middle
     * (fixDestination). The landing search started at the raw goal instead, found the roof's bedrock top as a safe
     * block and chose a spot above it that the flight cannot reach (take 4 of the elytra-roof film: the player circled
     * under the roof for 20 s). The search now starts from the destination the flight aims at.
     */
    @Test
    public void goalAboveTheRoofIsMovedInsideForAPlayerBelowIt() {
        DimensionType tall = dimensionType(true, 0, 256, 192);
        assertEquals(new BetterBlockPos(110, 96, 0), ElytraBehavior.fixDestination(tall, 150, new BetterBlockPos(110, 200, 0)));
        assertEquals("a goal inside the room stays", new BetterBlockPos(110, 120, 0), ElytraBehavior.fixDestination(tall, 150, new BetterBlockPos(110, 120, 0)));
        assertEquals("a player above the roof keeps the goal above it", new BetterBlockPos(110, 200, 0), ElytraBehavior.fixDestination(tall, 250, new BetterBlockPos(110, 200, 0)));
    }
}
