// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.registry.In;

@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class NestedTest {
    @In
    public static ModuleTestingHelper outerHelper;

    @In
    public static EntityManager outerManager;

    @Test
    public void outerTest() {
        Assertions.assertNotNull(outerHelper);
        Assertions.assertNotNull(outerManager);
    }

    @Nested
    class NestedTestClass {
        @In
        ModuleTestingHelper innerHelper;

        @In
        EntityManager innerManager;

        @Test
        public void innerTest() {
            Assertions.assertSame(innerManager, outerManager);
            Assertions.assertSame(innerHelper, outerHelper);
        }
    }
}
