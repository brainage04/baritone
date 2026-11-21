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

import baritone.api.utils.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** The walk look must point at the block the movement is heading for. */
public class MovementLookTest {

    private static final float EPSILON = 0.01f;

    @Test
    public void lookPointsAtTheTargetFromAnOffCentreStance() {
        // Standing 0.3 blocks off the centre of the block being walked to, 3 blocks away.
        Vec3 head = new Vec3(0.2, 1.62, 0.0);
        Vec3 dest = new Vec3(0.5, 0.0, 3.5);
        Rotation look = MovementHelper.lookTowards(head, dest, new Rotation(0.0f, 0.0f));
        assertEquals(-4.90f, look.getYaw(), 0.05f);
    }

    @Test
    public void lookKeepsTheCurrentPitch() {
        Vec3 head = new Vec3(0.5, 1.62, 0.5);
        Vec3 dest = new Vec3(0.5, 0.0, 3.5);
        Rotation look = MovementHelper.lookTowards(head, dest, new Rotation(0.0f, 12.5f));
        assertEquals(12.5f, look.getPitch(), EPSILON);
    }

    @Test
    public void lookIsNotSnappedToTheFortyFiveDegreeGrid() {
        // A target 30 degrees off the current yaw. The 45-degree snap aimed at -15 here, the grid
        // point nearest the current yaw, not at the target.
        Vec3 head = new Vec3(0.0, 1.62, 0.0);
        double radians = Math.toRadians(30.0);
        Vec3 dest = new Vec3(-Math.sin(radians) * 4.0, 0.0, Math.cos(radians) * 4.0);
        Rotation look = MovementHelper.lookTowards(head, dest, new Rotation(0.0f, 0.0f));
        assertEquals(30.0f, look.getYaw(), 0.05f);
    }

    @Test
    public void lookKeepsTheCurrentYawDirectlyAboveTheTarget() {
        // The horizontal offset is zero, so the yaw is undefined. Keeping the current yaw stops the
        // camera swinging 180 degrees as the player crosses the centre of the block below.
        Vec3 head = new Vec3(0.5, 3.0, 0.5);
        Vec3 dest = new Vec3(0.5, 0.0, 0.5);
        Rotation look = MovementHelper.lookTowards(head, dest, new Rotation(37.0f, -20.0f));
        assertEquals(37.0f, look.getYaw(), EPSILON);
        assertEquals(-20.0f, look.getPitch(), EPSILON);
    }

    @Test
    public void lookWrapsAcrossTheYawSeam() {
        // Target just past -180 from a player facing just short of +180. The result is the same
        // angle whichever side of the seam it is written on.
        Vec3 head = new Vec3(0.0, 1.62, 0.0);
        Vec3 dest = new Vec3(0.05, 0.0, -4.0);
        Rotation look = MovementHelper.lookTowards(head, dest, new Rotation(179.0f, 0.0f));
        assertEquals(-179.28f, Rotation.normalizeYaw(look.getYaw()), 0.05f);
    }
}
