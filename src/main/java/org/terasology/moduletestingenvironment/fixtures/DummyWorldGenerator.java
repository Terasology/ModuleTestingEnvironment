// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment.fixtures;

import org.terasology.engine.core.SimpleUri;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.generation.BaseFacetedWorldGenerator;
import org.terasology.engine.world.generation.WorldBuilder;
import org.terasology.engine.world.generator.RegisterWorldGenerator;
import org.terasology.engine.world.generator.plugin.WorldGeneratorPluginLibrary;

@RegisterWorldGenerator(id = "dummy", displayName = "dummy")
public class DummyWorldGenerator extends BaseFacetedWorldGenerator {

    @In
    private WorldGeneratorPluginLibrary worldGeneratorPluginLibrary;

    public DummyWorldGenerator(SimpleUri uri) {
        super(uri);
    }

    @Override
    protected WorldBuilder createWorld() {
        return new WorldBuilder(worldGeneratorPluginLibrary)
                .addProvider(new FlatSurfaceHeightProvider(40))
                .addPlugins();
    }
}