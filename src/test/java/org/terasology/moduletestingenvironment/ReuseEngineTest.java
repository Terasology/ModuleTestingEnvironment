// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.moduletestingenvironment.fixtures.DummyComponent;
import org.terasology.moduletestingenvironment.fixtures.DummyEvent;

import java.util.Set;

public class ReuseEngineTest {
    private static ModuleTestingHelper helper;
    private EntityRef entity;

    @BeforeAll
    public static void setup() throws Exception {
        helper = new ModuleTestingHelper() {
            @Override
            public Set<String> getDependencies() {
                return Sets.newHashSet("ModuleTestingEnvironment");
            }
        };
        helper.setup();
    }

    @AfterAll
    public static void tearDown() {
        helper.tearDown();
    }

    /**
     * Create a new entity for each test and store it in {@code entity}.
     */
    @BeforeEach
    public void prepareEntityForTest() {
        EntityManager entityManager = helper.getHostContext().get(EntityManager.class);
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
