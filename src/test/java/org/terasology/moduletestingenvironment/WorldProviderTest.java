/*
 * Copyright 2017 MovingBlocks
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

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.RelevanceRegionComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockManager;

import java.util.Set;

public class WorldProviderTest extends ModuleTestingEnvironment {

    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("engine", "ModuleTestingEnvironment");
    }

    @Test
    public void defaultWorldSetBlockTest() {
        WorldProvider worldProvider = getHostContext().get(WorldProvider.class);
        BlockManager blockManager = getHostContext().get(BlockManager.class);

        forceAndWaitForGeneration(Vector3i.zero());

        // this will change if the worldgenerator changes or the seed is altered, the main point is that this is a real
        // block type and not engine:unloaded
        Assert.assertEquals("engine:air", worldProvider.getBlock(0, 0 ,0).getURI().toString());

        // also verify that we can set and immediately get blocks from the worldprovider
        worldProvider.setBlock(Vector3i.zero(), blockManager.getBlock("engine:unloaded"));
        Assert.assertEquals("engine:unloaded", worldProvider.getBlock(0, 0 ,0).getURI().toString());
    }
}
