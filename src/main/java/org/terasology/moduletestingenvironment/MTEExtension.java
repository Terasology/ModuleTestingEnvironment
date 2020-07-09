/*
 * Copyright 2020 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.moduletestingenvironment;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.opentest4j.MultipleFailuresError;
import org.terasology.context.Context;
import org.terasology.engine.TerasologyEngine;
import org.terasology.engine.modes.StateMainMenu;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3i;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.moduletestingenvironment.extension.UseWorldGenerator;
import org.terasology.registry.In;
import org.terasology.rendering.opengl.ScreenGrabber;
import org.terasology.world.RelevanceRegionComponent;
import org.terasology.world.WorldProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Junit 5 Extension for using {@link ModuleTestingHelper} in your test.
 * <p>
 * Supports Terasology's DI as in usual Systems. You can inject Managers via {@link In} annotation, constructor's or
 * test's parameters. Also you can inject {@link ModuleTestingHelper} itself.
 * <p>
 * Every class annotated with this will create a single {@link ModuleTestingHelper} and use it during execution of
 * all tests in the class. This also means that all engine instances are shared between all tests in the class. If you
 * want isolated engine instances try {@link IsolatedMTEExtension}
 * <p>
 * Use this within {@link org.junit.jupiter.api.extension.ExtendWith}
 */
public class MTEExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver, TestInstancePostProcessor {

    protected final Map<Class<?>, ModuleTestingHelper> helperContexts = new HashMap<>();

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        helperContexts.get(context.getRequiredTestClass()).tearDown();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Dependencies dependencies = context.getRequiredTestClass().getAnnotation(Dependencies.class);
        UseWorldGenerator useWorldGenerator = context.getRequiredTestClass().getAnnotation(UseWorldGenerator.class);
        ModuleTestingHelper helperContext = new ModuleTestingHelper();
        if (dependencies != null) {
            helperContext.setDependencies(Sets.newHashSet(dependencies.value()));
        }
        if (useWorldGenerator != null) {
            helperContext.setWorldGeneratorUri(useWorldGenerator.value());
        }
        helperContext.setup();
        helperContexts.put(context.getRequiredTestClass(), helperContext);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ModuleTestingHelper helper = helperContexts.get(extensionContext.getRequiredTestClass());
        return helper.getHostContext().get(parameterContext.getParameter().getType()) != null
                || parameterContext.getParameter().getType().equals(ModuleTestingEnvironment.class)
                || parameterContext.getParameter().getType().equals(ModuleTestingHelper.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ModuleTestingHelper helper = helperContexts.get(extensionContext.getRequiredTestClass());
        Class<?> type = parameterContext.getParameter().getType();

        return getDIInstance(helper, type);
    }

    private Object getDIInstance(ModuleTestingHelper helper, Class<?> type) {
        if (type.equals(ModuleTestingHelper.class) || type.equals(ModuleTestingEnvironment.class)) {
            return helper;
        }
        return helper.getHostContext().get(type);
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
        ModuleTestingHelper helper = helperContexts.get(extensionContext.getRequiredTestClass());
        List<IllegalAccessException> exceptionList = new LinkedList<>();
        Class<?> type = testInstance.getClass();
        while (type != null) {
            Arrays.stream(type.getDeclaredFields())
                    .filter((field) -> field.getAnnotation(In.class) != null)
                    .peek((field) -> field.setAccessible(true))
                    .forEach((field) -> {
                        Object candidateObject = getDIInstance(helper, field.getType());
                        try {
                            field.set(testInstance, candidateObject);
                        } catch (IllegalAccessException e) {
                            exceptionList.add(e);
                        }
                    });

            type = type.getSuperclass();
        }
        // It is tests, then it is legal ;)
        if (!exceptionList.isEmpty()) {
            throw new MultipleFailuresError("I cannot provide DI instances:", exceptionList);
        }
    }
}
