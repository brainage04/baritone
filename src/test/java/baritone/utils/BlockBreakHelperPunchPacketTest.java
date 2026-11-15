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
 * Since 26.3 LivingEntity#swing only animates locally; the server learns about a swing from ServerboundPunchPacket,
 * which Minecraft#continueAttack sends next to the swing (Minecraft.java:1604-1606). A block break that only swings
 * leaves the server without the swing.
 */
public class BlockBreakHelperPunchPacketTest {

    private static final String PUNCH = "net/minecraft/network/protocol/game/ServerboundPunchPacket";

    @Test
    public void everySwingSendsThePunchPacket() throws IOException {
        List<String> calls = classCalls();
        assertTrue("BlockBreakHelper never swings, so this test proves nothing", calls.stream().anyMatch(c -> c.startsWith("swing ")));
        assertTrue("BlockBreakHelper swings without sending " + PUNCH + "; the server never sees the swing",
                calls.contains("getstatic " + PUNCH + ".INSTANCE"));
        assertTrue("BlockBreakHelper reads the punch packet but never sends it",
                calls.stream().anyMatch(c -> c.startsWith("send ")));
    }

    private static List<String> classCalls() throws IOException {
        byte[] bytes;
        try (InputStream in = BlockBreakHelperPunchPacketTest.class.getClassLoader()
                .getResourceAsStream("baritone/utils/BlockBreakHelper.class")) {
            assertNotNull("BlockBreakHelper.class is not on the test classpath", in);
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
        assertFalse("BlockBreakHelper has no code", calls.isEmpty());
        return calls;
    }
}
