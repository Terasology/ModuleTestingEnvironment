// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.context.Context;
import org.terasology.engine.TerasologyEngine;
import org.terasology.engine.modes.StateIngame;
import org.terasology.moduletestingenvironment.extension.Dependencies;

import java.io.IOException;
import java.util.List;

@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class ClientConnectionTest {

    @Test
    public void testClientConnection(ModuleTestingHelper helper) throws IOException {
        Context clientContext = helper.createClient();
        List<TerasologyEngine> engines = helper.getEngines();
        Assertions.assertEquals(2, engines.size());
        Assertions.assertAll(engines
                .stream()
                .map((engine) ->
                        () -> Assertions.assertEquals(StateIngame.class, engine.getState().getClass())));
    }
}
