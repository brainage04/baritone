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

package baritone.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 26.3 computes break speed in {@code Player#getDestroySpeed} (Player.java:583-608): dig speed effects, then
 * BLOCK_BREAK_SPEED, then SUBMERGED_MINING_SPEED while the eye is in water, then /5 when airborne. ToolSet only knew
 * the effects, so mining under water was estimated 5x too fast and airborne mining 5x too slow.
 */
public class ToolSetBreakSpeedTest {

    private static final double EPSILON = 1e-6; // the attributes are floats

    @Test
    public void dryGroundMiningIsUnscaled() {
        assertEquals(1.0, ToolSet.playerSpeedMultiplier(false, 0, -1, 1.0F, 0.2F, false, true), EPSILON);
    }

    @Test
    public void eyeInWaterAppliesSubmergedMiningSpeed() {
        assertEquals(0.2, ToolSet.playerSpeedMultiplier(false, 0, -1, 1.0F, 0.2F, true, true), EPSILON);
        // aqua affinity is an ADD_MULTIPLIED_TOTAL modifier on the attribute (Enchantments.java:315-320), so the
        // attribute value already carries it: 0.2 * (1 + 4) = 1.0
        assertEquals(1.0, ToolSet.playerSpeedMultiplier(false, 0, -1, 1.0F, 1.0F, true, true), EPSILON);
    }

    @Test
    public void airborneDividesByFive() {
        assertEquals(0.2, ToolSet.playerSpeedMultiplier(false, 0, -1, 1.0F, 0.2F, false, false), EPSILON);
    }

    @Test
    public void blockBreakSpeedAttributeScales() {
        assertEquals(2.5, ToolSet.playerSpeedMultiplier(false, 0, -1, 2.5F, 0.2F, false, true), EPSILON);
    }

    @Test
    public void digSpeedAndFatigueMatchTheGame() {
        // haste I: 1 + (0 + 1) * 0.2
        assertEquals(1.2, ToolSet.playerSpeedMultiplier(true, 0, -1, 1.0F, 0.2F, false, true), EPSILON);
        // haste II: 1 + (1 + 1) * 0.2
        assertEquals(1.4, ToolSet.playerSpeedMultiplier(true, 1, -1, 1.0F, 0.2F, false, true), EPSILON);
        // mining fatigue I: 0.3^(0+1)
        assertEquals(0.3, ToolSet.playerSpeedMultiplier(false, 0, 0, 1.0F, 0.2F, false, true), EPSILON);
        // mining fatigue III: 0.3^(2+1)
        assertEquals(0.027, ToolSet.playerSpeedMultiplier(false, 0, 2, 1.0F, 0.2F, false, true), EPSILON);
    }
}
