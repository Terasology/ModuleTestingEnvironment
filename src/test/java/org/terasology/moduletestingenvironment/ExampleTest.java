/*
 * Copyright 2017 MovingBlocks
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

import com.google.api.client.util.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.terasology.context.Context;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.logic.players.event.ResetCameraEvent;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.network.ClientComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockManager;

import java.util.List;
import java.util.Set;

public class ExampleTest  extends ModuleTestingEnvironment {
    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("engine", "ModuleTestingEnvironment");
    }

    @Test
    public void testExample() {
        WorldProvider worldProvider = getHostContext().get(WorldProvider.class);
        BlockManager blockManager = getHostContext().get(BlockManager.class);

        // create some clients (the library connects them automatically)
        Context clientContext1 = createClient();
        Context clientContext2 = createClient();

        // assert that both clients are known to the server
        EntityManager hostEntityManager = getHostContext().get(EntityManager.class);
        List<EntityRef> clientEntities = Lists.newArrayList(hostEntityManager.getEntitiesWith(ClientComponent.class));
        Assert.assertEquals(2, clientEntities.size());

        // send an event to a client's local player just for fun
        clientContext1.get(LocalPlayer.class).getClientEntity().send(new ResetCameraEvent());

        // wait for a chunk to be generated
        forceAndWaitForGeneration(Vector3i.zero());

        // set a block's type and immediately read it back
        worldProvider.setBlock(Vector3i.zero(), blockManager.getBlock("engine:air"));
        Assert.assertEquals("engine:air", worldProvider.getBlock(Vector3f.zero()).getURI().toString());
    }
}