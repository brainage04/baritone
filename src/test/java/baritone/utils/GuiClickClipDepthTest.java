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

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GuiClickClipDepthTest {

    private static final float Z_NEAR = 0.05F;
    private static final float Z_FAR = 1024.0F;

    @Test
    public void clickRayRunsFromTheNearPlaneToTheFarPlane() {
        for (boolean zZeroToOne : new boolean[]{true, false}) {
            // Built the way Projection#getMatrix builds the level projection: near and far swapped (reverse Z)
            Matrix4f inverse = new Matrix4f()
                    .setPerspective((float) Math.toRadians(70), 16F / 9F, Z_FAR, Z_NEAR, zZeroToOne)
                    .invert();
            assertEquals("near end, zZeroToOne=" + zZeroToOne, Z_NEAR, viewDistance(inverse, GuiClick.ClipDepth.near()), 0.01);
            assertEquals("far end, zZeroToOne=" + zZeroToOne, Z_FAR, viewDistance(inverse, GuiClick.ClipDepth.far(zZeroToOne)), Z_FAR * 0.01);
        }
    }

    private static float viewDistance(Matrix4f inverseProjection, float depth) {
        Vector4f pos = inverseProjection.transform(new Vector4f(0, 0, depth, 1));
        return -pos.z() / pos.w();
    }
}
