// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.moduletestingenvironment.fixtures.BaseTestingClass;

@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class SubclassInjectionTest extends BaseTestingClass {
    @Test
    public void testInjection() {
        // ensure the superclass's private fields were injected correctly
        Assertions.assertNotNull(getEntityManager());
        Assertions.assertNotNull(getHelper());
    }
}
