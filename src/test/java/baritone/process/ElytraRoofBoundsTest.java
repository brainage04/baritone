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
import net.minecraft.core.HolderSet;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.dimension.DimensionType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The elytra bounds used the literal 127 for the Nether roof and gated it on {@code Level.NETHER}, so a datapack
 * dimension with {@code has_ceiling} and a different {@code logical_height} got the wrong ceiling. 26.3 decides the
 * roof by data: the Nether's dimension type is minY 0, height 256, logicalHeight 128, hasCeiling true, and vanilla
 * uses {@code minY + logicalHeight - 1} for it (PortalForcer.java:59, ServerLevel#getLogicalHeight).
 */
public class ElytraRoofBoundsTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static DimensionType dimensionType(boolean hasCeiling, int minY, int height, int logicalHeight) {
        return new DimensionType(
                false, false, hasCeiling, false, 1.0, minY, height, logicalHeight,
                HolderSet.empty(), 0.0F,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.NONE, CardinalLighting.Type.DEFAULT,
                EnvironmentAttributeMap.EMPTY, HolderSet.empty(), Optional.empty());
    }

    @Test
    public void vanillaNetherRoofIs127() {
        DimensionType nether = dimensionType(true, 0, 256, 128);
        assertEquals(127, ElytraProcess.roofY(nether));
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
}
