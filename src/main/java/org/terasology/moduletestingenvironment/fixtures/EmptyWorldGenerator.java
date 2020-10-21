// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment.fixtures;

import org.terasology.engine.SimpleUri;
import org.terasology.registry.In;
import org.terasology.world.generation.BaseFacetedWorldGenerator;
import org.terasology.world.generation.WorldBuilder;
import org.terasology.world.generator.RegisterWorldGenerator;
import org.terasology.world.generator.plugin.WorldGeneratorPlugin;
import org.terasology.world.generator.plugin.WorldGeneratorPluginLibrary;

import java.util.ArrayList;
import java.util.List;

/**
 * Bare World Generator to generating empty chunks for testing.
 */
@RegisterWorldGenerator(id = "empty", displayName = "empty")
public class EmptyWorldGenerator extends BaseFacetedWorldGenerator {
    @In
    private WorldGeneratorPluginLibrary worldGeneratorPluginLibrary;

    public EmptyWorldGenerator(SimpleUri uri) {
        super(uri);
    }

    @Override
    protected WorldBuilder createWorld() {
        return new WorldBuilder(new WorldGeneratorPluginLibrary() {
            @Override
            public <U extends WorldGeneratorPlugin> List<U> instantiateAllOfType(Class<U> ofType) {
                return new ArrayList<>();
            }
        });
    }
}
