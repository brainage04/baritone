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

package baritone.launch;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.InvokeInstruction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * LookBehavior restores the camera rotation at the POST player update. Since 26.3 the move packet is built in
 * LocalPlayer#sendChanges, which Minecraft#tick calls after the entity tick, so POST has to fire after that call or
 * the server is sent the restored rotation instead of Baritone's.
 */
public class PlayerUpdatePostOrderTest {

    private static final String SEND_CHANGES = "net/minecraft/client/player/LocalPlayer.sendChanges()V";

    @Test
    public void playerUpdatePostFiresAfterTheMovePacketIsSent() throws IOException {
        String postTarget = postUpdateTarget();
        List<String> calls = tickCalls();
        int post = calls.indexOf(postTarget);
        int send = calls.indexOf(SEND_CHANGES);
        assertTrue("Minecraft#tick does not call " + postTarget, post >= 0);
        assertTrue("Minecraft#tick does not call " + SEND_CHANGES, send >= 0);
        assertTrue("POST player update is injected after " + postTarget + ", which Minecraft#tick calls before "
                + SEND_CHANGES + "; the server would get the rotation LookBehavior restores at POST", post >= send);
    }

    private static String postUpdateTarget() throws IOException {
        String source = Files.readString(Path.of("src/launch/java/baritone/launch/mixins/MixinMinecraft.java"));
        int method = source.indexOf("private void postUpdateEntities(");
        assertTrue("postUpdateEntities not found in MixinMinecraft", method >= 0);
        Matcher matcher = Pattern.compile("target\\s*=\\s*\"([^\"]+)\"").matcher(source.substring(0, method));
        String target = null;
        while (matcher.find()) {
            target = matcher.group(1);
        }
        assertNotNull("No injection target before postUpdateEntities", target);
        if (target.startsWith("L") && target.contains(";")) {
            int semicolon = target.indexOf(';');
            return target.substring(1, semicolon) + "." + target.substring(semicolon + 1);
        }
        return target;
    }

    private static List<String> tickCalls() throws IOException {
        byte[] bytes;
        try (InputStream in = PlayerUpdatePostOrderTest.class.getClassLoader().getResourceAsStream("net/minecraft/client/Minecraft.class")) {
            assertNotNull("Minecraft.class is not on the test classpath", in);
            bytes = in.readAllBytes();
        }
        // The JDK's class-file API, not ASM: asm_version 9.7 rejects 26.3's Java 25 class files ("Unsupported class
        // file major version 69").
        List<String> calls = new ArrayList<>();
        for (MethodModel method : ClassFile.of().parse(bytes).methods()) {
            if (!method.methodName().equalsString("tick") || !method.methodType().equalsString("()V")) {
                continue;
            }
            method.code().ifPresent(code -> code.forEach(element -> {
                if (element instanceof InvokeInstruction invoke) {
                    calls.add(invoke.owner().asInternalName() + "." + invoke.name().stringValue() + invoke.type().stringValue());
                }
            }));
        }
        return calls;
    }
}
