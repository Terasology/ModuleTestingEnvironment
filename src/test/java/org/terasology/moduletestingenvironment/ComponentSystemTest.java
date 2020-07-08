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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.moduletestingenvironment.fixtures.DummyComponent;
import org.terasology.moduletestingenvironment.fixtures.DummyEvent;
import org.terasology.registry.In;

@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class ComponentSystemTest {
    @In
    private EntityManager entityManager;

    @Test
    public void simpleEventTest() {
        EntityRef entity = entityManager.create(new DummyComponent());
        entity.send(new DummyEvent());
        Assertions.assertTrue(entity.getComponent(DummyComponent.class).dummy);
    }
}
