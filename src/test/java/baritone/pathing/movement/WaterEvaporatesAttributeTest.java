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
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A water bucket is useless where water evaporates: BucketItem#emptyContents (BucketItem.java:122) turns the placed
 * water into smoke. 26.3 decides that with EnvironmentAttributes.WATER_EVAPORATES, which is syncable
 * (EnvironmentAttributes.java:121-123), so a client reads the server's value. Baritone asked
 * Level#dimension() == Level.NETHER instead, which is right only for the vanilla Nether: a datapack dimension that
 * evaporates water but is not the Nether still got the bucket, and a Nether clone that does not evaporate lost it.
 */
public class WaterEvaporatesAttributeTest {

    private static final BlockPos POS = new BlockPos(0, 64, 0);

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void evaporatingDimensionRefusesTheBucket() {
        assertFalse(MovementHelper.waterBucketWorks(readerWith(true), POS));
    }

    @Test
    public void nonEvaporatingDimensionAllowsTheBucket() {
        assertTrue(MovementHelper.waterBucketWorks(readerWith(false), POS));
    }

    @Test
    public void bucketSitesDoNotReadTheDimension() throws IOException {
        for (String className : List.of("baritone/pathing/movement/CalculationContext.class",
                "baritone/pathing/movement/movements/MovementFall.class")) {
            List<String> calls = classCalls(className);
            assertFalse(className + " still reads Level.NETHER instead of the evaporation attribute",
                    calls.contains("getstatic net/minecraft/world/level/Level.NETHER"));
            assertFalse(className + " still asks Level#dimension instead of the evaporation attribute",
                    calls.stream().anyMatch(c -> c.startsWith("dimension ")));
        }
    }

    private static EnvironmentAttributeReader readerWith(boolean evaporates) {
        return new EnvironmentAttributeReader() {
            @Override
            public <Value> Value getDimensionValue(EnvironmentAttribute<Value> attribute) {
                return value(attribute);
            }

            @Override
            public <Value> Value getValue(EnvironmentAttribute<Value> attribute, Vec3 pos, net.minecraft.world.attribute.SpatialAttributeInterpolator interpolator) {
                return value(attribute);
            }

            @SuppressWarnings("unchecked")
            private <Value> Value value(EnvironmentAttribute<Value> attribute) {
                return attribute == EnvironmentAttributes.WATER_EVAPORATES ? (Value) (Boolean) evaporates : attribute.defaultValue();
            }
        };
    }

    private static List<String> classCalls(String resource) throws IOException {
        byte[] bytes;
        try (InputStream in = WaterEvaporatesAttributeTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(resource + " is not on the test classpath", in);
            bytes = in.readAllBytes();
        }
        List<String> calls = new ArrayList<>();
        for (MethodModel method : ClassFile.of().parse(bytes).methods()) {
            method.code().ifPresent(code -> code.forEach(element -> {
                if (element instanceof InvokeInstruction invoke) {
                    calls.add(invoke.name().stringValue() + " " + invoke.owner().asInternalName());
                } else if (element instanceof FieldInstruction field) {
                    calls.add("getstatic " + field.owner().asInternalName() + "." + field.name().stringValue());
                }
            }));
        }
        assertFalse(resource + " has no code", calls.isEmpty());
        return calls;
    }
}
