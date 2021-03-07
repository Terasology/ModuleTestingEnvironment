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

import com.google.common.collect.Lists;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.engine.context.Context;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.logic.players.event.ResetCameraEvent;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.engine.network.ClientComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.BlockManager;


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
        helper.runUntil(()-> Lists.newArrayList(entityManager.getEntitiesWith(ClientComponent.class)).size() == 2);
        Assertions.assertEquals(2, Lists.newArrayList(entityManager.getEntitiesWith(ClientComponent.class)).size());

        // run until a condition is true or until a timeout passes
        boolean timedOut = helper.runWhile(1000, ()-> true);
        Assertions.assertTrue(timedOut);

        // send an event to a client's local player just for fun
        clientContext1.get(LocalPlayer.class).getClientEntity().send(new ResetCameraEvent());

        // wait for a chunk to be generated
        helper.forceAndWaitForGeneration(new Vector3i());

        // set a block's type and immediately read it back
        worldProvider.setBlock(new Vector3i(), blockManager.getBlock("engine:air"));
        Assertions.assertEquals("engine:air", worldProvider.getBlock(new Vector3f()).getURI().toString());
    }
}
