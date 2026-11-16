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
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.OptionalLong;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A water bucket is useless where water evaporates: BucketItem#emptyContents turns the placed water into smoke in a
 * dimension whose type is ultraWarm.
 */
public class WaterBucketDimensionTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static DimensionType dimensionType(boolean ultraWarm) {
        return new DimensionType(
                OptionalLong.empty(), true, false, ultraWarm, true, 1.0, true, false,
                0, 256, 256, BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F,
                new DimensionType.MonsterSettings(false, true, ConstantInt.of(0), 0));
    }

    @Test
    public void evaporatingDimensionRefusesTheBucket() {
        assertFalse(MovementHelper.waterBucketWorks(dimensionType(true)));
    }

    @Test
    public void nonEvaporatingDimensionAllowsTheBucket() {
        assertTrue(MovementHelper.waterBucketWorks(dimensionType(false)));
    }
}
