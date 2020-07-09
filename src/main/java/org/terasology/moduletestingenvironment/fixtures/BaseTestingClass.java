// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment.fixtures;

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.registry.In;

// A dummy class for testing injection of super class fields
public class BaseTestingClass {
    @In
    private EntityManager entityManager;

    @In
    private ModuleTestingHelper helper;

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public ModuleTestingHelper getHelper() {
        return helper;
    }
}
