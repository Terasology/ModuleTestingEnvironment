/*
 * Copyright 2018 MovingBlocks
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
import org.terasology.context.Context;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.chat.ChatMessageEvent;
import org.terasology.logic.players.LocalPlayer;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageSentTest extends ModuleTestingEnvironment {

    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("engine", "ModuleTestingEnvironment");
    }

    @Test
    public void messageSentAndReceivedTest() {
        Context senderContext = createClient();
        Context targetContext = createClient();

        LocalPlayer sender = senderContext.get(LocalPlayer.class);
        LocalPlayer target = targetContext.get(LocalPlayer.class);

        try (TestEventReceiver<ChatMessageEvent> receiver = new TestEventReceiver<>(targetContext, ChatMessageEvent.class)) {
            List<EntityRef> actualEntities = receiver.getEntityRefs();
            assertTrue(actualEntities.isEmpty());
            for (int i = 0; i < 5; i++) {
                target.getClientEntity().send(new ChatMessageEvent("Test message #" + i, sender.getClientInfoEntity()));
                assertEquals(i + 1, actualEntities.size());
                assertEquals(target.getClientEntity(), actualEntities.get(i));
            }
        }
    }
}
