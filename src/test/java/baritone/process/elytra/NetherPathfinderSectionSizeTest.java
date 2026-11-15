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

package baritone.process.elytra;

import net.minecraft.SharedConstants;
import net.minecraft.core.SectionPos;
import net.minecraft.server.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

/**
 * The pathfinder's octree holds one bit per block of a chunk section, so the all-solid fill in
 * NetherPathfinderContext#writeChunkData has to write one bit per block. Upstream 26.3 has SECTION_SIZE = 16 there,
 * which fills 2 bytes of a 512-byte section and leaves the rest of an all-solid section readable as air.
 */
public class NetherPathfinderSectionSizeTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void sectionSizeIsTheBlockCountOfAChunkSection() throws ReflectiveOperationException {
        Field field = NetherPathfinderContext.class.getDeclaredField("SECTION_SIZE");
        field.setAccessible(true);
        int sectionSize = field.getInt(null);
        assertEquals("SECTION_SIZE is the number of blocks in a chunk section, not its edge length",
                SectionPos.SECTION_BLOCK_COUNT, sectionSize);
        assertEquals("a 16x16x16 section", SectionPos.SECTION_SIZE * SectionPos.SECTION_SIZE * SectionPos.SECTION_SIZE, sectionSize);
    }
}
