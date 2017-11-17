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
package org.terasology.moduletestingenvironment.fixtures;

import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.Event;
import org.terasology.logic.inventory.events.DropItemEvent;
import org.terasology.math.geom.Vector3f;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;
import org.terasology.moduletestingenvironment.TestEventReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

public class TestEventReceiverTest extends ModuleTestingEnvironment {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("engine", "ModuleTestingEnvironment");
    }

    @Test
    public void cumulativeDropsTest() {
        final List<EntityRef> referenceDrops = new ArrayList<>();
        try (TestEventReceiver<DropItemEvent> spy = new TestEventReceiver<>(getHostContext(), DropItemEvent.class)) {
            List<EntityRef> drops = spy.getEntityRefs();
            assertTrue(drops.isEmpty());
            for (int i = 0; i < 5; i++) {
                referenceDrops.add(dropItem());
                assertEquals(i + 1, drops.size());
                assertEquals(referenceDrops.get(i), drops.get(i));
            }
        }
    }

    @Test
    public void properClosureTest() {
        final List<EntityRef> drops;
        try (TestEventReceiver<DropItemEvent> spy = new TestEventReceiver<>(getHostContext(), DropItemEvent.class)) {
            drops = spy.getEntityRefs();
        }
        dropItem();
        assertTrue(drops.isEmpty());
    }

    @Test
    public void userCallbackTest() {
        final List<DropItemEvent> events = new ArrayList<>();

        TestEventReceiver receiver = new TestEventReceiver<>(getHostContext(), DropItemEvent.class, (event, entity) -> {
            events.add(event);
        });

        for (int i = 0; i < 3; i++) {
            dropItem();
        }

        // send a dummy event that should not get counted
        EntityRef.NULL.send(new DummyEvent());

        // ensure all interesting events were caught
        assertEquals(3, events.size());

        // shouldn't receive events after closing
        receiver.close();
        dropItem();
        assertEquals(3, events.size());
    }

    /**
     * Drops a generic item into the world.
     *
     * @return the item
     */
    private EntityRef dropItem() {
        final EntityManager entityManager = getHostContext().get(EntityManager.class);
        final EntityRef item = entityManager.create("engine:iconItem");
        item.send(new DropItemEvent(Vector3f.zero()));
        return item;
    }
}
