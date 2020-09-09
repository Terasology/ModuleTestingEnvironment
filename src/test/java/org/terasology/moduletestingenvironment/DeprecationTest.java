// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.engine.context.Context;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.logic.players.event.ResetCameraEvent;
import org.terasology.engine.network.ClientComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.moduletestingenvironment.extension.Dependencies;


/**
 * The ExampleTest but using the old (deprecated) API
 */
@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class DeprecationTest {

    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;
    @In
    private EntityManager entityManager;
    @In
    private ModuleTestingEnvironment helper;

    @Test
    public void testExample() {
        // create some clients (the library connects them automatically)
        Context clientContext1 = helper.createClient();
        Context clientContext2 = helper.createClient();

        // wait for both clients to be known to the server
        helper.runUntil(() -> Lists.newArrayList(entityManager.getEntitiesWith(ClientComponent.class)).size() == 2);
        Assertions.assertEquals(2, Lists.newArrayList(entityManager.getEntitiesWith(ClientComponent.class)).size());

        // run until a condition is true or until a timeout passes
        boolean timedOut = helper.runWhile(1000, () -> true);
        Assertions.assertTrue(timedOut);

        // send an event to a client's local player just for fun
        clientContext1.get(LocalPlayer.class).getClientEntity().send(new ResetCameraEvent());

        // wait for a chunk to be generated
        helper.forceAndWaitForGeneration(Vector3i.zero());

        // set a block's type and immediately read it back
        worldProvider.setBlock(Vector3i.zero(), blockManager.getBlock("engine:air"));
        Assertions.assertEquals("engine:air", worldProvider.getBlock(Vector3f.zero()).getURI().toString());
    }
}
