/*
 * Copyright 2020 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.math.geom.Vector3i;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockManager;

@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class WorldProviderTest {

    @In
    WorldProvider worldProvider;
    @In
    BlockManager blockManager;
    @In
    ModuleTestingEnvironment moduleTestingEnvironment;

    @Test
    public void defaultWorldSetBlockTest() {
        moduleTestingEnvironment.forceAndWaitForGeneration(Vector3i.zero());

        // this will change if the worldgenerator changes or the seed is altered, the main point is that this is a real
        // block type and not engine:unloaded
        Assertions.assertEquals("engine:air", worldProvider.getBlock(0, 0, 0).getURI().toString());

        // also verify that we can set and immediately get blocks from the worldprovider
        worldProvider.setBlock(Vector3i.zero(), blockManager.getBlock("engine:unloaded"));
        Assertions.assertEquals("engine:unloaded", worldProvider.getBlock(0, 0, 0).getURI().toString());
    }
}
