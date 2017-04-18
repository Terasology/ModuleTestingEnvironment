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
import org.junit.Before;
import org.junit.Test;
import org.terasology.assets.management.AssetManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.moduletestingenvironment.fixtures.DummyComponent;
import org.terasology.moduletestingenvironment.fixtures.DummyEvent;
import org.terasology.utilities.Assets;

import java.util.Set;

public class AssetLoadingTest extends ModuleTestingEnvironment {

    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("engine", "Core", "ModuleTestingEnvironment");
    }

    @Test
    public void simpleLoadingTest() throws InterruptedException {
        AssetManager assetManager = context.get(AssetManager.class);
        Assert.assertNotNull(assetManager.getAsset("Core:axe", Prefab.class).get());
    }
}
