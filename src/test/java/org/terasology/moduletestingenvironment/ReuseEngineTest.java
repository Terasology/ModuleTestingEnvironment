/*
 * Copyright 2020 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.moduletestingenvironment.fixtures.DummyComponent;
import org.terasology.moduletestingenvironment.fixtures.DummyEvent;

import java.util.Set;

public class ReuseEngineTest {
    private static ModuleTestingEnvironment mte;
    private EntityRef entity;

    @BeforeAll
    public static void setup() throws Exception {
        mte = new ModuleTestingEnvironment() {
            @Override
            public Set<String> getDependencies() {
                return Sets.newHashSet("ModuleTestingEnvironment");
            }
        };
        mte.setup();
    }

    @AfterAll
    public static void tearDown() {
        mte.tearDown();
    }

    /**
     * Create a new entity for each test and store it in {@code entity}.
     */
    @BeforeEach
    public void prepareEntityForTest() {
        EntityManager entityManager = mte.getHostContext().get(EntityManager.class);
        entity = entityManager.create(new DummyComponent());
    }

    @Test
    public void someTest() {
        entity.send(new DummyEvent());
        Assert.assertTrue(entity.getComponent(DummyComponent.class).dummy);
    }

    @Test
    public void someOtherTest() {
        Assert.assertTrue(entity.hasComponent(DummyComponent.class));
    }
}
