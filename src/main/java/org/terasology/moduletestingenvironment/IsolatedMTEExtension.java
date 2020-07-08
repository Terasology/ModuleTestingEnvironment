// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.terasology.registry.In;

/**
 * Subclass of {@link MTEExtension} which isolates all test cases by creating a new engine for each test. This is much
 * slower since it runs the startup and shutdown process for all tests. You should use {@link MTEExtension} unless
 * you're certain that you need to use this class.
 * 
 * Use this within {@link org.junit.jupiter.api.extension.ExtendWith}
 */
public class IsolatedMTEExtension extends MTEExtension implements BeforeAllCallback, AfterAllCallback,
        AfterEachCallback, ParameterResolver, TestInstancePostProcessor {
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // don't call super
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // don't call super
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        super.afterAll(context);
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
        // beforeEach would be run after postProcess so postProcess would NPE, so we initialize the MTE here beforehand
        super.beforeAll(extensionContext);
        super.postProcessTestInstance(testInstance, extensionContext);
    }
}
