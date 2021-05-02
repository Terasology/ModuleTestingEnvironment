// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.moduletestingenvironment.extension.Dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.terasology.engine.testUtil.Assertions.assertNotEmpty;

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
        assertNotNull(block);
        assertEquals(0, block.getHardness());
        assertEquals("Air", block.getDisplayName());
    }

    @Test
    public void simpleLoadingTest() {
        assertNotEmpty(assetManager.getAsset("engine:test", Prefab.class));
    }
}
