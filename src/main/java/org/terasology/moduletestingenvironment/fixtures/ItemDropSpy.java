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

import org.terasology.context.Context;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.event.internal.EventSystem;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.logic.inventory.events.DropItemEvent;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test fixture for recording item drops.
 * <p>
 * Two usage patterns are supported.  A test may instantiate an {@code ItemDropSpy}, execute some
 * code, and then examine the list of drops provided by {@link #getDrops()}.  In order to handle
 * registration and un-registration of the event handler listening for item drops, this should be
 * done in a try-with-resources block ({@code ItemDropSpy} is {@link AutoCloseable}):
 * {@code
 * try (ItemDropSpy spy = new ItemDropSpy(getHostContext())) {
 *   dropExampleItems();
 *   assertTrue(spy.getDrops().exampleItemsAreCorrect());
 * }
 * }
 * <p>
 * Alternatively, a test may call the static method {@link #collectDrops(Context, Runnable)}, which
 * may be easier to understand, especially if the code being run is a single method.  In this style,
 * the previous example would look like this:
 * {@code
 *   List<EntityRef> drops = ItemDropSpy.collectDrops(getHostContext(), ::dropExampleItems);
 *   assertTrue(drops.exampleItemsAreCorrect());
 * }
 */
public class ItemDropSpy extends BaseComponentSystem implements AutoCloseable {

  private EventSystem eventSystem;

  private List<EntityRef> drops = new ArrayList<>();

  /**
   * Constructs a new {@code ItemDropSpy} and registers it to listen for item drops.
   *
   * @param context the context object for the test; this should probably be obtained through
   *                {@link ModuleTestingEnvironment#getHostContext()} and is needed so we can
   *                obtain an {@link EventSystem} instance to register our event handler.
   */
  public ItemDropSpy(Context context) {
    eventSystem = context.get(EventSystem.class);
    eventSystem.registerEventHandler(this);
  }

  /** Unregisters this {@code ItemDropSpy} so it stops listening for item drops. */
  public void close() {
    eventSystem.unregisterEventHandler(this);
  }

  /**
   * Returns a read-only view of the list of items dropped.
   * <p>
   * If the {@code ItemDropSpy} has not been {@linkplain #close() closed}, then this list will
   * continue to be updated if further item drops occur.
   */
  public List<EntityRef> getDrops() {
    return Collections.unmodifiableList(drops);
  }

  /**
   * Runs {@code block} and returns the list of all drops that occurred in the process.
   *
   * @param context the context object for the test, probably obtained using
   *                {@link ModuleTestingEnvironment#getDependencies()}
   * @param block the code to run
   * @return the list of all items dropped during execution of {@code block}
   */
  public static List<EntityRef> collectDrops(Context context, Runnable block) {
    List<EntityRef> drops;
    try (ItemDropSpy spy = new ItemDropSpy(context)) {
      drops = spy.getDrops();
      block.run();
    }
    return drops;
  }

  /**
   * Records the item drop.
   * <p>
   * Note that this doesn't put the item in an inventory or otherwise interfere with the drop
   * itself, but it does store a reference to the item.  Consequently, the item still exists in the
   * world, and if other actors modify or destroy it, those changes would be reflected in the list
   * of drops.
   */
  @ReceiveEvent
  public void onItemDrop(DropItemEvent event, EntityRef item) {
    drops.add(item);
  }
}
