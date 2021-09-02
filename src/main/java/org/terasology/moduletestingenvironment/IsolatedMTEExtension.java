// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Subclass of {@link MTEExtension} which isolates all test cases by creating a new engine for each test. This is much
 * slower since it runs the startup and shutdown process for all tests. You should use {@link MTEExtension} unless
 * you're certain that you need to use this class.
 * <p>
 * Use this within {@link org.junit.jupiter.api.extension.ExtendWith}
 */
public class IsolatedMTEExtension extends MTEExtension {
    @Override
    protected ExtensionContext.Namespace getNamespace(ExtensionContext context) {
        return ExtensionContext.Namespace.create(MTEExtension.class, context.getTestMethod());
    }
}
