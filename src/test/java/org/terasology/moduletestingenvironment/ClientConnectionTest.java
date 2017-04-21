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
import org.junit.Assert;
import org.junit.Test;
import org.terasology.engine.TerasologyEngine;
import org.terasology.engine.modes.StateIngame;

import java.util.Set;

public class ClientConnectionTest extends ModuleTestingEnvironment {
    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("engine", "Core", "ModuleTestingEnvironment");
    }

    @Test
    public void testClientConnection() {
        TerasologyEngine client = createClient();
        connectToHost(client);
        Assert.assertEquals(StateIngame.class, client.getState().getClass());
    }
}
