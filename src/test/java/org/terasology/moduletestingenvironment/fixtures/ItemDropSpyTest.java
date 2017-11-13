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
import org.terasology.logic.inventory.events.DropItemEvent;
import org.terasology.math.geom.Vector3f;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ItemDropSpyTest extends ModuleTestingEnvironment {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("engine", "ModuleTestingEnvironment");
    }

    @Test
    public void cumulativeDropsTest() {
        final List<EntityRef> referenceDrops = new ArrayList<>();
        try (ItemDropSpy spy = new ItemDropSpy(getHostContext())) {
            List<EntityRef> drops = spy.getDrops();
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
        try (ItemDropSpy spy = new ItemDropSpy(getHostContext())) {
            drops = spy.getDrops();
        }
        dropItem();
        assertTrue(drops.isEmpty());
    }

    @Test
    public void staticUsageTest() {
        // This forward declaration is wrapped in an atom so it can be final and the lambda will close over it.
        final AtomicReference<List<EntityRef>> referenceDropsAtom = new AtomicReference<>();
        final List<EntityRef> staticDrops = ItemDropSpy.collectDrops(getHostContext(), () -> {
            try (ItemDropSpy referenceSpy = new ItemDropSpy(getHostContext())) {
                referenceDropsAtom.set(referenceSpy.getDrops());
                dropItem();
            }
        });
        assertEquals(referenceDropsAtom.get().size(), staticDrops.size());
        assertEquals(referenceDropsAtom.get().get(0), staticDrops.get(0));
    }

    @Test
    public void readOnlyTest() {
        final List<EntityRef> drops = ItemDropSpy.collectDrops(getHostContext(), this::dropItem);
        exception.expect(UnsupportedOperationException.class);
        drops.add(EntityRef.NULL);
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
