package baritone.process;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;

/**
 * ElytraBehavior chose the BuildLimitPathFinder from {@code dimension() == Level.NETHER}, so a datapack dimension with
 * {@code has_ceiling} got the normal finder, whose search the roof cuts off. The game's own answer to "does this
 * dimension have a roof" is DimensionType#hasCeiling.
 */
public class ElytraPathFinderChoiceTest {

    @Test
    public void pathFinderChoiceAsksForACeiling() throws IOException {
        String source = Files.readString(Path.of("src/main/java/baritone/process/elytra/ElytraBehavior.java"));
        assertFalse("the path finder must be chosen by DimensionType#hasCeiling, not the dimension key",
                source.contains("dimension() == Level.NETHER"));
    }
}
