// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment.fixtures;


import org.terasology.gestalt.entitysystem.component.Component;

public class DummyComponent implements Component<DummyComponent> {
    public boolean eventReceived = false;
    public String name;

    @Override
    public void copyFrom(DummyComponent other) {
        eventReceived = other.eventReceived;
        name = other.name;
    }
}
