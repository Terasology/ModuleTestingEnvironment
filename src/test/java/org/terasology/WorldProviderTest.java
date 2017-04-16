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
package org.terasology;

import org.junit.Assert;
import org.junit.Test;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockManager;

public class WorldProviderTest extends ModuleTestingEnvironment {
    @Test
    public void defaultWorldTest() {
        WorldProvider worldProvider = context.get(WorldProvider.class);
        Assert.assertEquals("engine:stone", worldProvider.getBlock(0, -1 ,0).getURI());
        Assert.assertEquals("engine:dirt", worldProvider.getBlock(0, 0 ,0).getURI());
        Assert.assertEquals("engine:air", worldProvider.getBlock(0, 1 ,0).getURI());
    }

    @Test
    public void defaultWorldSetBlockTest() {
        WorldProvider worldProvider = context.get(WorldProvider.class);
        BlockManager blockManager = context.get(BlockManager.class);
        worldProvider.setBlock(new Vector3i(0, 1, 0), blockManager.getBlock("engine:dirt"));
        Assert.assertEquals("engine:dirt", worldProvider.getBlock(0, 1 ,0).getURI());
    }
}
