// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.subsystem.headless.mode.StateHeadlessSetup;
import org.terasology.game.GameManifest;
import org.terasology.module.DependencyInfo;
import org.terasology.module.DependencyResolver;
import org.terasology.module.Module;
import org.terasology.module.ResolutionResult;
import org.terasology.naming.Name;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.internal.WorldInfo;
import org.terasology.world.time.WorldTime;

import java.util.Collection;
import java.util.Set;

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

        Set<Name> dependencyNames = Sets.newHashSet();
        for (String moduleName : dependencies) {
            logger.warn("Adding dependencies for {}", moduleName);
            dependencyNames.add(new Name(moduleName));
            recursivelyAddModuleDependencies(dependencyNames, new Name(moduleName));
        }

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

    private void recursivelyAddModuleDependencies(Set<Name> modules, Name moduleName) {
        Module module = CoreRegistry.get(ModuleManager.class).getRegistry().getLatestModuleVersion(moduleName);
        if (module != null) {
            for (DependencyInfo dependencyInfo : module.getMetadata().getDependencies()) {
                logger.warn("Adding dependency {}", dependencyInfo.getId());
                modules.add(dependencyInfo.getId());
                recursivelyAddModuleDependencies(modules, dependencyInfo.getId());
            }
        }
    }
}
