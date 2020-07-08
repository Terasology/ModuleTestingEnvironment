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

import com.google.common.collect.Sets;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.opentest4j.MultipleFailuresError;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.moduletestingenvironment.extension.UseWorldGenerator;
import org.terasology.registry.In;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Junit 5 Extension for using {@link ModuleTestingEnvironment} in your test.
 * <p>
 * Supports Terasology's DI as in usual Systems. You can inject Managers via {@link In} annotation, constructor's or
 * test's parameters. Also you can inject {@link ModuleTestingEnvironment} itself.
 * <p>
 * Every testclass used this create one {@link ModuleTestingEnvironment} and use it during execution all tests in
 * class.
 * <p>
 * Use this within {@link org.junit.jupiter.api.extension.ExtendWith}
 */
public class MTEExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver, TestInstancePostProcessor {

    private final Map<Class<?>, ModuleTestingEnvironment> mteContexts = new HashMap<>();

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        mteContexts.get(context.getRequiredTestClass()).tearDown();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Dependencies dependencies = context.getRequiredTestClass().getAnnotation(Dependencies.class);
        UseWorldGenerator useWorldGenerator = context.getRequiredTestClass().getAnnotation(UseWorldGenerator.class);
        ModuleTestingEnvironment mteContext = new ModuleTestingEnvironment();
        if (dependencies != null) {
            mteContext.setDependencies(Sets.newHashSet(dependencies.value()));
        }
        if (useWorldGenerator != null) {
            mteContext.setWorldGeneratorUri(useWorldGenerator.value());
        }
        mteContext.setup();
        mteContexts.put(context.getRequiredTestClass(), mteContext);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ModuleTestingEnvironment mte = mteContexts.get(extensionContext.getRequiredTestClass());
        return mte.getHostContext().get(parameterContext.getParameter().getType()) != null
                || parameterContext.getParameter().getType().equals(ModuleTestingEnvironment.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ModuleTestingEnvironment mte = mteContexts.get(extensionContext.getRequiredTestClass());
        Class<?> type = parameterContext.getParameter().getType();

        return getDIInstance(mte, type);
    }

    private Object getDIInstance(ModuleTestingEnvironment mte, Class<?> type) {
        if (type.equals(ModuleTestingEnvironment.class)) {
            return mte;
        }
        return mte.getHostContext().get(type);
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
        ModuleTestingEnvironment mte = mteContexts.get(extensionContext.getRequiredTestClass());
        List<IllegalAccessException> exceptionList = new LinkedList<>();
        Arrays.stream(testInstance.getClass().getDeclaredFields())
                .filter((field) -> field.getAnnotation(In.class) != null)
                .peek((field) -> field.setAccessible(true))
                .forEach((field) -> {
                    Object candidateObject = getDIInstance(mte, field.getType());
                    try {
                        field.set(testInstance, candidateObject);
                    } catch (IllegalAccessException e) {
                        exceptionList.add(e);
                    }
                });
        // It is tests, then it is legal ;)
        if (!exceptionList.isEmpty()) {
            throw new MultipleFailuresError("I cannot provide DI instances:", exceptionList);
        }
    }
}
