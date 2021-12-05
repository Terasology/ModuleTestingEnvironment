// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which {@index "world generator"} to use.
 *
 * @see org.terasology.moduletestingenvironment.MTEExtension
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseWorldGenerator {

    /**
     * The URN of the world generator, e.g. <code>"CoreWorlds:facetedPerlin"</code>
     */
    String value();
}
