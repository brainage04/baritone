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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A tick on which Baritone does not click (the aim, one tick behind the player's eye, is off the block) used to end the
 * break: the next click went to the client as a fresh start, and the game sets the break progress back to 0 for a fresh
 * start (MultiPlayerGameMode.java:169-197 in 1.21.11). Mining from the water's surface, where the eye moves every tick,
 * lost the first blocks' progress eight times in a row.
 */
public class BlockBreakHelperTest {

    @Test
    public void aClickedTickThatDidNotBreakTheBlockIsInProgress() {
        BlockBreakHelper.BreakStreak streak = new BlockBreakHelper.BreakStreak();
        assertFalse(streak.inProgress());
        streak.hit(true);
        assertTrue(streak.inProgress());
        streak.hit(false); // the click broke the block
        assertFalse(streak.inProgress());
    }

    @Test
    public void aShortRunOfMissedTicksKeepsTheBreak() {
        BlockBreakHelper.BreakStreak streak = new BlockBreakHelper.BreakStreak();
        streak.hit(true);
        for (int i = 0; i < BlockBreakHelper.MAX_MISSED_TICKS; i++) {
            streak.miss();
            assertTrue("the break was given up after " + (i + 1) + " missed ticks", streak.inProgress());
        }
    }

    @Test
    public void aLongRunOfMissedTicksGivesTheBreakUp() {
        BlockBreakHelper.BreakStreak streak = new BlockBreakHelper.BreakStreak();
        streak.hit(true);
        for (int i = 0; i <= BlockBreakHelper.MAX_MISSED_TICKS; i++) {
            streak.miss();
        }
        assertFalse(streak.inProgress());
    }

    @Test
    public void aClickStartsTheCountAgain() {
        BlockBreakHelper.BreakStreak streak = new BlockBreakHelper.BreakStreak();
        streak.hit(true);
        for (int i = 0; i < BlockBreakHelper.MAX_MISSED_TICKS; i++) {
            streak.miss();
        }
        streak.hit(true);
        for (int i = 0; i < BlockBreakHelper.MAX_MISSED_TICKS; i++) {
            streak.miss();
        }
        assertTrue(streak.inProgress());
    }

    @Test
    public void missedTicksDoNotStartABreak() {
        BlockBreakHelper.BreakStreak streak = new BlockBreakHelper.BreakStreak();
        streak.miss();
        assertFalse(streak.inProgress());
    }
}
