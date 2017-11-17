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

import org.terasology.context.Context;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.internal.EntityInfoComponent;
import org.terasology.entitySystem.event.Event;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.event.internal.EventReceiver;
import org.terasology.entitySystem.event.internal.EventSystem;
import org.terasology.entitySystem.systems.BaseComponentSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Test helper for listening to {@link Event}s
 * <p>
 * A test should instantiate a {@code TestEventReceiver}, execute some code, and then examine the list of entityRefs or
 * events provided by {@link #getEntityRefs()} and {@link #getEvents()}.
 * <p>
 * Users can optionally supply a {@link BiConsumer} to handle the events with custom logic.
 * <pre>
 * {@literal
 * TestEventReceiver receiver = new TestEventReceiver<>(context, DropItemEvent.class, (event, entity) -> {
 *   // do something with the event or entity
 * });
 * }
 * </pre>
 *
 * You can automatically unregister your receiver using a try-with-resources block ({@code
 * TestEventReceiver} is {@link AutoCloseable}):
 *
 * <pre>
 * {@literal
 * try (TestEventReceiver<DropItemEvent> spy = new TestEventReceiver<>(getHostContext(), DropItemEvent.class)) {
 *   drops = spy.getEntityRefs();
 * }
 * }
 * </pre>
 */
public class TestEventReceiver<T extends Event> implements AutoCloseable, EventReceiver<T>{

    private EventSystem eventSystem;
    private Class<T> klass;
    private List<EntityRef> entityRefs = new ArrayList<>();
    private List<T> events = new ArrayList<>();
    private BiConsumer<T, EntityRef> biConsumer = (a,b)-> { };

    /**
     * Constructs a new {@code TestEventReceiver} and registers it to listen for events.
     *
     * @param context the context object for the test; this should probably be obtained through
     *                {@link ModuleTestingEnvironment#getHostContext()} and is needed so we can
     *                obtain an {@link EventSystem} instance to register our event handler.
     */
    public TestEventReceiver(Context context, Class<T> klass, BiConsumer<T, EntityRef> biConsumer) {
        this.biConsumer = biConsumer;
        this.klass = klass;
        eventSystem = context.get(EventSystem.class);
        eventSystem.registerEventReceiver(this, klass, EntityInfoComponent.class);
    }

    public TestEventReceiver(Context context, Class<T> klass) {
        this.klass = klass;
        eventSystem = context.get(EventSystem.class);
        eventSystem.registerEventReceiver(this, klass, EntityInfoComponent.class);
    }

    /** Unregisters this {@code TestEventReceiver} so it stops listening for events. */
    public void close() {
        eventSystem.unregisterEventReceiver(this, klass, EntityInfoComponent.class);
    }

    /**
     * Returns a read-only view of the list of entities which are sent events.
     * <p>
     * If the {@code TestEventReceiver} has not been {@linkplain #close() closed}, then this list will
     * continue to be updated if further events occur.
     */
    public List<EntityRef> getEntityRefs() {
        return Collections.unmodifiableList(entityRefs);
    }

    /**
     * Returns a read-only view of the list of events.
     * <p>
     * If the {@code TestEventReceiver} has not been {@linkplain #close() closed}, then this list will
     * continue to be updated if further events occur.
     */
    public List<T> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Records the event.
     * <p>
     * Note that this doesn't put the entity in an inventory or otherwise interfere with the event
     * itself, but it does store a reference to the entity and event.  Consequently, the entity still exists in
     * the world, and if other actors modify or destroy it, those changes would be reflected in the
     * list of entityRefs.
     */
    @ReceiveEvent
    public void onEvent(T event, EntityRef entity) {
        biConsumer.accept(event, entity);
        events.add(event);
        entityRefs.add(entity);
    }
}
