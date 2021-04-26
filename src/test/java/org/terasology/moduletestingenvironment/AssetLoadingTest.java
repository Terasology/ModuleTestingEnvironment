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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.assets.management.AssetManager;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;

import java.util.Optional;

@Tag("MteTest")
@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class AssetLoadingTest {

    @In
    private BlockManager blockManager;
    @In
    private AssetManager assetManager;

    @Test
    public void blockPrefabLoadingTest() {
        Block block = blockManager.getBlock("engine:air");
        Assertions.assertNotNull(block);
        Assertions.assertEquals(0, block.getHardness());
        Assertions.assertEquals("Air", block.getDisplayName());
    }

    @Test
    public void simpleLoadingTest() {
        Assertions.assertNotEquals(assetManager.getAsset("engine:test", Prefab.class), Optional.empty());
    }
}
