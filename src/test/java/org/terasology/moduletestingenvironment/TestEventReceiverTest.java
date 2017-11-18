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

import com.google.common.collect.Sets;
import org.junit.Test;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.moduletestingenvironment.fixtures.DummyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestEventReceiverTest extends ModuleTestingEnvironment {
    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("engine", "ModuleTestingEnvironment");
    }

    @Test
    public void repeatedEventTest() {
        final List<EntityRef> expectedEntities = new ArrayList<>();
        try (TestEventReceiver<DummyEvent> receiver = new TestEventReceiver<>(getHostContext(), DummyEvent.class)) {
            List<EntityRef> actualEntities = receiver.getEntityRefs();
            assertTrue(actualEntities.isEmpty());
            for (int i = 0; i < 5; i++) {
                expectedEntities.add(sendEvent());
                assertEquals(i + 1, actualEntities.size());
                assertEquals(expectedEntities.get(i), actualEntities.get(i));
            }
        }
    }

    @Test
    public void properClosureTest() {
        final List<EntityRef> entities;
        try (TestEventReceiver<DummyEvent> receiver = new TestEventReceiver<>(getHostContext(), DummyEvent.class)) {
            entities = receiver.getEntityRefs();
        }
        sendEvent();
        assertTrue(entities.isEmpty());
    }

    @Test
    public void userCallbackTest() {
        final List<DummyEvent> events = new ArrayList<>();

        TestEventReceiver receiver = new TestEventReceiver<>(getHostContext(), DummyEvent.class, (event, entity) -> {
            events.add(event);
        });

        for (int i = 0; i < 3; i++) {
            sendEvent();
        }

        // ensure all interesting events were caught
        assertEquals(3, events.size());

        // shouldn't receive events after closing
        receiver.close();
        sendEvent();
        assertEquals(3, events.size());
    }

    /**
     * Drops a generic item into the world.
     *
     * @return the item
     */
    private EntityRef sendEvent() {
        final EntityRef entityRef = getHostContext().get(EntityManager.class).create();
        entityRef.send(new DummyEvent());
        return entityRef;
    }
}
