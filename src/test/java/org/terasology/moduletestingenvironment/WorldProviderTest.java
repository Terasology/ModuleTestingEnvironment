// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.math.geom.Vector3i;
import org.terasology.moduletestingenvironment.extension.Dependencies;

@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class WorldProviderTest {

    @In
    WorldProvider worldProvider;
    @In
    BlockManager blockManager;
    @In
    ModuleTestingHelper helper;

    @Test
    public void defaultWorldSetBlockTest() {
        helper.forceAndWaitForGeneration(Vector3i.zero());

        // this will change if the worldgenerator changes or the seed is altered, the main point is that this is a real
        // block type and not engine:unloaded
        Assertions.assertEquals("engine:air", worldProvider.getBlock(0, 0, 0).getURI().toString());

        // also verify that we can set and immediately get blocks from the worldprovider
        worldProvider.setBlock(Vector3i.zero(), blockManager.getBlock("engine:unloaded"));
        Assertions.assertEquals("engine:unloaded", worldProvider.getBlock(0, 0, 0).getURI().toString());
    }
}
