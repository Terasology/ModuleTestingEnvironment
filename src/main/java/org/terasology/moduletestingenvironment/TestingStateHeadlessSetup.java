// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.SimpleUri;
import org.terasology.engine.core.TerasologyConstants;
import org.terasology.engine.core.module.ModuleManager;
import org.terasology.engine.core.subsystem.headless.mode.StateHeadlessSetup;
import org.terasology.engine.game.GameManifest;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.world.internal.WorldInfo;
import org.terasology.engine.world.time.WorldTime;
import org.terasology.module.DependencyResolver;
import org.terasology.module.Module;
import org.terasology.module.ResolutionResult;
import org.terasology.naming.Name;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class TestingStateHeadlessSetup extends StateHeadlessSetup {
    private static final Logger logger = LoggerFactory.getLogger(TestingStateHeadlessSetup.class);
    private final Collection<String> dependencies;
    private final String worldGeneratorUri;
    public TestingStateHeadlessSetup(Collection<String> dependencies, String worldGeneratorUri) {
        this.dependencies = dependencies;
        this.worldGeneratorUri = worldGeneratorUri;
    }

    @Override
    public GameManifest createGameManifest() {
        GameManifest gameManifest = new GameManifest();

        gameManifest.setTitle("testworld");
        gameManifest.setSeed("seed");
        DependencyResolver resolver = new DependencyResolver(CoreRegistry.get(ModuleManager.class).getRegistry());

        Set<Name> dependencyNames = dependencies.stream().map(Name::new).collect(Collectors.toSet());
        logger.info("Building manifest for module dependencies: {}", dependencyNames);

        // Include the MTE module to provide world generators and suchlike.
        dependencyNames.add(new Name("ModuleTestingEnvironment"));

        ResolutionResult result = resolver.resolve(dependencyNames);
        if (!result.isSuccess()) {
            logger.error("Unable to resolve modules: {}", dependencyNames);
        }

        for (Module module : result.getModules()) {
            logger.info("Loading module {} {}", module.getId(), module.getVersion());
            gameManifest.addModule(module.getId(), module.getVersion());
        }

        float timeOffset = 0.25f + 0.025f;  // Time at dawn + little offset to spawn in a brighter env.
        WorldInfo worldInfo = new WorldInfo(TerasologyConstants.MAIN_WORLD, gameManifest.getSeed(),
                (long) (WorldTime.DAY_LENGTH * timeOffset), new SimpleUri(worldGeneratorUri));
        gameManifest.addWorld(worldInfo);
        return gameManifest;
    }
}
