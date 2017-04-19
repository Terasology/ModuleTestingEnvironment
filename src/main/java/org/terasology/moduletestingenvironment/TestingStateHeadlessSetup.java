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
import org.terasology.engine.SimpleUri;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.subsystem.headless.mode.StateHeadlessSetup;
import org.terasology.game.GameManifest;
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
    private Collection<String> dependencies;
    public TestingStateHeadlessSetup(Collection<String> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public GameManifest createGameManifest() {

        GameManifest gameManifest = new GameManifest();

        gameManifest.setTitle("testworld");
        gameManifest.setSeed("seed");
        DependencyResolver resolver = new DependencyResolver(CoreRegistry.get(ModuleManager.class).getRegistry());

        Set<Name> dependencyNames = Sets.newHashSet();
        for(String moduleName : dependencies) {
            dependencyNames.add(new Name(moduleName));
        }

        ResolutionResult result = resolver.resolve(dependencyNames);
        if (!result.isSuccess()) {
            return null;
        }

        for (Module module : result.getModules()) {
            gameManifest.addModule(module.getId(), module.getVersion());
        }

        float timeOffset = 0.25f + 0.025f;  // Time at dawn + little offset to spawn in a brighter env.
        WorldInfo worldInfo = new WorldInfo(TerasologyConstants.MAIN_WORLD, gameManifest.getSeed(),
                (long) (WorldTime.DAY_LENGTH * timeOffset), new SimpleUri("core:flat"));
        gameManifest.addWorld(worldInfo);
        return gameManifest;
    }
}
