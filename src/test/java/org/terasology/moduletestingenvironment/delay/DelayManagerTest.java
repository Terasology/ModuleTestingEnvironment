// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment.delay;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.context.Context;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.moduletestingenvironment.MTEExtension;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.moduletestingenvironment.TestEventReceiver;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.network.ClientComponent;
import org.terasology.registry.In;

@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class DelayManagerTest {

    @In
    DelayManager delayManager;

    @In
    EntityManager entityManager;

    @Test
    public void delayedActionIsTriggeredTest(ModuleTestingHelper helper) {
        Context clientContext = helper.createClient();
        helper.runWhile(() -> Lists.newArrayList(entityManager.getEntitiesWith(ClientComponent.class)).isEmpty());

        final TestEventReceiver<DelayedActionTriggeredEvent> eventReceiver =
                new TestEventReceiver<>(clientContext, DelayedActionTriggeredEvent.class);

        EntityRef player = clientContext.get(LocalPlayer.class).getClientEntity();
        delayManager.addDelayedAction(player, "ModuleTestingEnvironment:delayManagerTest", 1000);

        Assertions.assertTrue(eventReceiver.getEvents().isEmpty());
        helper.runWhile(1200, () -> true);
        Assertions.assertFalse(eventReceiver.getEvents().isEmpty());
    }
}
