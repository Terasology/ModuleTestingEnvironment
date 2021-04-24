// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.opentest4j.MultipleFailuresError;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.module.ModuleManager;
import org.terasology.engine.registry.In;
import org.terasology.moduletestingenvironment.extension.Dependencies;
import org.terasology.moduletestingenvironment.extension.UseWorldGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 * Note that classes marked {@link Nested} will share the engine context with their parent.
 * <p>
 * This will configure the logger and the current implementation is not subtle or polite about it, see
 * {@link #setupLogging()} for notes.
 * <p>
 * Use this within {@link org.junit.jupiter.api.extension.ExtendWith}
 */
public class MTEExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver, TestInstancePostProcessor {

    static final String LOGBACK_RESOURCE = "default-logback.xml";

    protected final Map<Class<?>, ModuleTestingHelper> helperContexts = new HashMap<>();

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        if (testClass.isAnnotationPresent(Nested.class)) {
            // nested classes get torn down in the parent
            return;
        }

        // Could be null if an exception interrupts before setup is complete.
        if (helperContexts.get(testClass) != null) {
            helperContexts.get(testClass).tearDown();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        System.setProperty(ModuleManager.LOAD_CLASSPATH_MODULES_PROPERTY, "true");

        if (context.getRequiredTestClass().isAnnotationPresent(Nested.class)) {
            // nested classes get set up in the parent
            ModuleTestingHelper parentHelper = helperContexts.get(context.getRequiredTestClass().getEnclosingClass());
            helperContexts.put(context.getRequiredTestClass(), parentHelper);
            return;
        }

        setupLogging();

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

    /**
     * Apply our default logback configuration to the logger.
     * <p>
     * Modules won't generally have their own logback-test.xml, so we'll install ours from {@value LOGBACK_RESOURCE}.
     * <p>
     * <b>TODO:</b>
     * <ul>
     *   <li>Only reset the current LoggerContext if it really hasn't been customized by elsewhere.
     *   <li>When there are multiple classes with MTEExtension, do we end up doing this repeatedly
     *       in the same process?
     *   <li>Provide a way to add/change/override what this is doing that doesn't require checking
     *       out the MTE sources and editing default-logback.xml.
     * </ul>
     */
    void setupLogging() {
        // This is mostly right out of the book:
        //   http://logback.qos.ch/xref/chapters/configuration/MyApp3.html
        JoranConfigurator cfg = new JoranConfigurator();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        cfg.setContext(context);
        try (InputStream i = getClass().getResourceAsStream(LOGBACK_RESOURCE)) {
            if (i == null) {
                throw new RuntimeException("Failed to find " + LOGBACK_RESOURCE);
            }
            cfg.doConfigure(i);
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + LOGBACK_RESOURCE, e);
        } catch (JoranException e) {
            throw new RuntimeException("Error during logger configuration", e);
        } finally {
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        }
    }
}
