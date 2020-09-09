// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment.fixtures;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.Share;

@Share(DummySystem.class)
@RegisterSystem(RegisterMode.AUTHORITY)
public class DummySystem extends BaseComponentSystem {
    @ReceiveEvent
    public void onDummyEvent(DummyEvent event, EntityRef entity) {
        DummyComponent component = entity.getComponent(DummyComponent.class);
        component.dummy = true;
        entity.saveComponent(component);
    }
}
