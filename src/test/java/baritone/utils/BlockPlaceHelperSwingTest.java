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

import net.minecraft.world.InteractionResult;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.FieldInstruction;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Minecraft#startUseItem swings for any InteractionResult.Success whose swing source is PREDICTED, not only for the
 * SUCCESS singleton (Minecraft.java:1716-1743). BlockItem#place returns the singleton, but Entity#interactOn and
 * friends return SUCCESS.withoutItem(), a different Success instance, so an identity comparison skips the swing.
 */
public class BlockPlaceHelperSwingTest {

    @Test
    public void derivedSuccessStillSwings() {
        assertTrue("the SUCCESS singleton swings", BlockPlaceHelper.shouldSwing(InteractionResult.SUCCESS));
        assertTrue("SUCCESS.withoutItem() is a different Success instance and must still swing",
                BlockPlaceHelper.shouldSwing(InteractionResult.SUCCESS.withoutItem()));
    }

    @Test
    public void serverOnlyAndNoneDoNotSwing() {
        assertFalse("SUCCESS_SERVER means the server sends the animation",
                BlockPlaceHelper.shouldSwing(InteractionResult.SUCCESS_SERVER));
        assertFalse("CONSUME has swing source NONE", BlockPlaceHelper.shouldSwing(InteractionResult.CONSUME));
        assertFalse(BlockPlaceHelper.shouldSwing(InteractionResult.FAIL));
        assertFalse(BlockPlaceHelper.shouldSwing(InteractionResult.PASS));
    }

    @Test
    public void tickDoesNotCompareTheSuccessSingleton() throws IOException {
        List<String> fields = tickFields();
        assertFalse("BlockPlaceHelper#tick compares InteractionResult.SUCCESS by identity, so a derived Success skips "
                + "the swing; test instanceof Success and read swingSource() instead",
                fields.contains("net/minecraft/world/InteractionResult.SUCCESS"));
    }

    private static List<String> tickFields() throws IOException {
        byte[] bytes;
        try (InputStream in = BlockPlaceHelperSwingTest.class.getClassLoader()
                .getResourceAsStream("baritone/utils/BlockPlaceHelper.class")) {
            assertNotNull("BlockPlaceHelper.class is not on the test classpath", in);
            bytes = in.readAllBytes();
        }
        List<String> fields = new ArrayList<>();
        for (MethodModel method : ClassFile.of().parse(bytes).methods()) {
            if (!method.methodName().equalsString("tick")) {
                continue;
            }
            method.code().ifPresent(code -> code.forEach(element -> {
                if (element instanceof FieldInstruction field) {
                    fields.add(field.owner().asInternalName() + "." + field.name().stringValue());
                }
            }));
        }
        assertFalse("BlockPlaceHelper#tick has no code", fields.isEmpty());
        return fields;
    }
}
