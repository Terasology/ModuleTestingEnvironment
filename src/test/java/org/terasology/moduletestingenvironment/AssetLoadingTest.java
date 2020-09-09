// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.moduletestingenvironment.extension.Dependencies;

import java.util.Optional;

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
