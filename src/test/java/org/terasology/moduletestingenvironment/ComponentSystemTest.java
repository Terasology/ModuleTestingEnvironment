// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.registry.In;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.moduletestingenvironment.fixtures.DummyComponent;
import org.terasology.moduletestingenvironment.fixtures.DummyEvent;

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
