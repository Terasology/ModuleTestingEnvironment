// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.moduletestingenvironment.fixtures.DummyComponent;
import org.terasology.moduletestingenvironment.fixtures.DummyEvent;
import org.terasology.registry.In;

import java.util.Set;

@ExtendWith(IsolatedMTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class IsolatedEngineTest {
    private Set<EntityManager> entityManagerSet = Sets.newHashSet();
    private EntityRef entity;

    @In
    private EntityManager entityManager;

    @BeforeEach
    public void prepareEntityForTest() {
        entity = entityManager.create(new DummyComponent());
    }

    @Test
    public void someTest() {
        // make sure we don't reuse the EntityManager
        Assert.assertFalse(entityManagerSet.contains(entityManager));
        entityManagerSet.add(entityManager);

        entity.send(new DummyEvent());
        Assert.assertTrue(entity.getComponent(DummyComponent.class).dummy);
    }

    @Test
    public void someOtherTest() {
        // make sure we don't reuse the EntityManager
        Assert.assertFalse(entityManagerSet.contains(entityManager));
        entityManagerSet.add(entityManager);

        Assert.assertFalse(entity.getComponent(DummyComponent.class).dummy);
    }
}
