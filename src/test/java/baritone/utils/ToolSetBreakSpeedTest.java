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
 * The game computes break speed in {@code Player#getDestroySpeed} (Player.java:732-762): dig speed effects, then
 * BLOCK_BREAK_SPEED, then SUBMERGED_MINING_SPEED while the eye is in water, then /5 when airborne. ToolSet only knew
 * the effects, so mining with the eye under water was estimated 5x too fast and any BLOCK_BREAK_SPEED modifier was
 * invisible to the estimate. The submerged factor depends on where the block is mined from, so it is not part of the
 * multiplier tested here: see {@code MiningCostSubmergedTest}.
 */
public class ToolSetBreakSpeedTest {

    private static final double EPSILON = 1e-6; // the attributes are floats

    @Test
    public void noEffectsAndNoAttributeIsUnscaled() {
        assertEquals(1.0, ToolSet.playerSpeedMultiplier(false, 0, -1, 1.0F), EPSILON);
    }

    @Test
    public void blockBreakSpeedAttributeScales() {
        assertEquals(2.5, ToolSet.playerSpeedMultiplier(false, 0, -1, 2.5F), EPSILON);
    }

    @Test
    public void digSpeedMatchesTheGame() {
        // haste I: 1 + (0 + 1) * 0.2
        assertEquals(1.2, ToolSet.playerSpeedMultiplier(true, 0, -1, 1.0F), EPSILON);
        // haste II: 1 + (1 + 1) * 0.2
        assertEquals(1.4, ToolSet.playerSpeedMultiplier(true, 1, -1, 1.0F), EPSILON);
    }

    /**
     * The game's table, not 0.3^(amplifier+1): amplifier 2 is 0.0027, not 0.027, and everything above it is 8.1E-4
     * (Player.java:743-749). Elder Guardians apply amplifier 2.
     */
    @Test
    public void fatigueMatchesTheGameTable() {
        assertEquals(0.3, ToolSet.playerSpeedMultiplier(false, 0, 0, 1.0F), EPSILON);
        assertEquals(0.09, ToolSet.playerSpeedMultiplier(false, 0, 1, 1.0F), EPSILON);
        assertEquals(0.0027, ToolSet.playerSpeedMultiplier(false, 0, 2, 1.0F), EPSILON);
        assertEquals(8.1E-4, ToolSet.playerSpeedMultiplier(false, 0, 3, 1.0F), EPSILON);
        assertEquals(8.1E-4, ToolSet.playerSpeedMultiplier(false, 0, 9, 1.0F), EPSILON);
    }
}
