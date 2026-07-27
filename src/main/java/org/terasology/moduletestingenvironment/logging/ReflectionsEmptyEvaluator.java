// Copyright 2026 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

import java.util.regex.Pattern;

/**
 * Replaces the Janino-based {@code reflectionsEmpty.matches(message)} expression previously used
 * by default-logback.xml's EvaluatorFilter to suppress Reflections' "given scan urls are empty"
 * warnings - JaninoEventEvaluator was removed in Logback 1.5.13.
 */
public class ReflectionsEmptyEvaluator extends ContextAwareBase implements EventEvaluator<ILoggingEvent>, LifeCycle {

    private static final Pattern REFLECTIONS_EMPTY = Pattern.compile("given scan urls are empty");

    private String name;
    private boolean started;

    @Override
    public boolean evaluate(ILoggingEvent event) throws EvaluationException {
        return REFLECTIONS_EMPTY.matcher(event.getFormattedMessage()).find();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }
}
