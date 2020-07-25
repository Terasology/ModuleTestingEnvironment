// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment.delay;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.moduletestingenvironment.MTEExtension;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.moduletestingenvironment.TestEventReceiver;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.network.ClientComponent;
import org.terasology.registry.In;

@ExtendWith(MTEExtension.class)
@Dependencies({"engine", "ModuleTestingEnvironment"})
public class DelayManagerTest {
    private static final Logger logger = LoggerFactory.getLogger(DelayManagerTest.class);

    @In
    DelayManager delayManager;

    @In
    EntityManager entityManager;

    @In
    Time time;

    @Test
    public void delayedActionIsTriggeredTest(ModuleTestingHelper helper) {
        helper.createClient();
        helper.runWhile(() -> Lists.newArrayList(entityManager.getEntitiesWith(ClientComponent.class)).isEmpty());

        final TestEventReceiver<DelayedActionTriggeredEvent> eventReceiver =
                new TestEventReceiver<>(helper.getHostContext(), DelayedActionTriggeredEvent.class);

        EntityRef player = Lists.newArrayList(entityManager.getEntitiesWith(ClientComponent.class)).get(0);
        delayManager.addDelayedAction(player, "ModuleTestingEnvironment:delayManagerTest", 1000);

        Assertions.assertTrue(eventReceiver.getEvents().isEmpty());

        long stop = time.getGameTimeInMs() + 1200;
        helper.runWhile(() -> time.getGameTimeInMs() < stop);
        Assertions.assertFalse(eventReceiver.getEvents().isEmpty());
    }
}
