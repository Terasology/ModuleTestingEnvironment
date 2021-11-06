// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the modules to load in the environment.
 *
 * @see org.terasology.moduletestingenvironment.MTEExtension
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependencies {
    /**
     * Names of modules, as defined by the <code>id</code> in their module.txt.
     */
    String[] value();
}
