// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.config.Config;
import org.terasology.engine.config.SystemConfig;
import org.terasology.engine.context.Context;
import org.terasology.engine.core.GameEngine;
import org.terasology.engine.core.PathManager;
import org.terasology.engine.core.PathManagerProvider;
import org.terasology.engine.core.TerasologyConstants;
import org.terasology.engine.core.TerasologyEngine;
import org.terasology.engine.core.TerasologyEngineBuilder;
import org.terasology.engine.core.modes.GameState;
import org.terasology.engine.core.modes.StateIngame;
import org.terasology.engine.core.modes.StateLoading;
import org.terasology.engine.core.modes.StateMainMenu;
import org.terasology.engine.core.module.ModuleManager;
import org.terasology.engine.core.subsystem.EngineSubsystem;
import org.terasology.engine.core.subsystem.headless.HeadlessAudio;
import org.terasology.engine.core.subsystem.headless.HeadlessGraphics;
import org.terasology.engine.core.subsystem.headless.HeadlessInput;
import org.terasology.engine.core.subsystem.headless.HeadlessTimer;
import org.terasology.engine.core.subsystem.headless.mode.HeadlessStateChangeListener;
import org.terasology.engine.core.subsystem.lwjgl.LwjglAudio;
import org.terasology.engine.core.subsystem.lwjgl.LwjglGraphics;
import org.terasology.engine.core.subsystem.lwjgl.LwjglInput;
import org.terasology.engine.core.subsystem.lwjgl.LwjglTimer;
import org.terasology.engine.network.JoinStatus;
import org.terasology.engine.network.NetworkSystem;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.rendering.opengl.ScreenGrabber;
import org.terasology.engine.rendering.world.viewDistance.ViewDistance;
import org.terasology.engine.testUtil.WithUnittestModule;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleMetadataJsonAdapter;
import org.terasology.gestalt.module.ModuleRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Manages game engines for tests.
 * <p>
 * There is always one engine that serves as the host. There may also be additional engines
 * simulating remote clients.
 * <p>
 * Most tests run with a single host and do not need to make direct references to this class.
 * <p>
 * This class is available via dependency injection with the {@link org.terasology.engine.registry.In} annotation
 * or as a parameter to a JUnit {@link org.junit.jupiter.api.Test} method; see {@link MTEExtension}.
 *
 * <h2>Client Engine Instances</h2>
 * Client instances can be easily created via {@link #createClient} which returns the in-game context of the created
 * engine instance. When this method returns, the client will be in the {@link StateIngame} state and connected to the
 * host. Currently all engine instances are headless, though it is possible to use headed engines in the future.
 */
public class Engines {
    private static final Logger logger = LoggerFactory.getLogger(Engines.class);

    protected final Set<String> dependencies = Sets.newHashSet("engine", "unittest");
    protected String worldGeneratorUri = ModuleTestingEnvironment.DEFAULT_WORLD_GENERATOR;
    protected boolean doneLoading;
    protected Context hostContext;
    protected final List<TerasologyEngine> engines = Lists.newArrayList();

    PathManager pathManager;
    PathManagerProvider.Cleaner pathManagerCleaner;
    TerasologyEngine host;

    public Engines(Set<String> dependencies, String worldGeneratorUri) {
        this.dependencies.addAll(dependencies);

        if (worldGeneratorUri != null) {
            this.worldGeneratorUri = worldGeneratorUri;
        }
    }

    /**
     * Set up and start the engine as configured via this environment.
     * <p>
     * Every instance should be shut down properly by calling {@link #tearDown()}.
     */
    protected void setup() {
        mockPathManager();
        try {
            host = createHost();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ScreenGrabber grabber = Mockito.mock(ScreenGrabber.class);
        hostContext.put(ScreenGrabber.class, grabber);
        CoreRegistry.put(GameEngine.class, host);
    }

    /**
     * Shut down a previously started testing environment.
     * <p>
     * Used to properly shut down and clean up a testing environment set up and started with {@link #setup()}.
     */
    protected void tearDown() {
        engines.forEach(TerasologyEngine::shutdown);
        engines.forEach(TerasologyEngine::cleanup);
        engines.clear();
        try {
            pathManagerCleaner.close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        host = null;
        hostContext = null;
    }

    /**
     * Creates a new client and connects it to the host.
     *
     * @return the created client's context object
     */
    public Context createClient(MainLoop mainLoop) throws IOException {
        TerasologyEngine terasologyEngine = createHeadlessEngine();
        terasologyEngine.getFromEngineContext(Config.class).getRendering().setViewDistance(ViewDistance.LEGALLY_BLIND);

        terasologyEngine.changeState(new StateMainMenu());
        connectToHost(terasologyEngine, mainLoop);
        Context context = terasologyEngine.getState().getContext();
        context.put(ScreenGrabber.class, hostContext.get(ScreenGrabber.class));
        return terasologyEngine.getState().getContext();
    }

    /**
     * The engines active in this instance of the module testing environment.
     * <p>
     * Engines are created for the host and connecting clients.
     *
     * @return list of active engines
     */
    public List<TerasologyEngine> getEngines() {
        return Lists.newArrayList(engines);
    }

    /**
     * Get the host context for this module testing environment.
     * <p>
     * The host context will be null if the testing environment has not been set up via {@link #setup()}
     * beforehand.
     *
     * @return the engine's host context, or null if not set up yet
     */
    public Context getHostContext() {
        return hostContext;
    }

    TerasologyEngine createHeadlessEngine() throws IOException {
        TerasologyEngineBuilder terasologyEngineBuilder = new TerasologyEngineBuilder();
        terasologyEngineBuilder
                .add(new WithUnittestModule())
                .add(new HeadlessGraphics())
                .add(new HeadlessTimer())
                .add(new HeadlessAudio())
                .add(new HeadlessInput());

        return createEngine(terasologyEngineBuilder);
    }

    @SuppressWarnings("unused")
    TerasologyEngine createHeadedEngine() throws IOException {
        EngineSubsystem audio = new LwjglAudio();
        TerasologyEngineBuilder terasologyEngineBuilder = new TerasologyEngineBuilder()
                .add(new WithUnittestModule())
                .add(audio)
                .add(new LwjglGraphics())
                .add(new LwjglTimer())
                .add(new LwjglInput());

        return createEngine(terasologyEngineBuilder);
    }

    TerasologyEngine createEngine(TerasologyEngineBuilder terasologyEngineBuilder) throws IOException {
        System.setProperty(ModuleManager.LOAD_CLASSPATH_MODULES_PROPERTY, "true");

        // create temporary home paths so the MTE engines don't overwrite config/save files in your real home path
        // FIXME: Collisions when attempting to do multiple simultaneous createEngines.
        //    (PathManager will need to be set in Context, not a process-wide global.)
        Path path = Files.createTempDirectory("terasology-mte-engine");
        PathManager.getInstance().useOverrideHomePath(path);
        logger.info("Created temporary engine home path: {}", path);

        // JVM will delete these on normal termination but not exceptions.
        path.toFile().deleteOnExit();

        TerasologyEngine terasologyEngine = terasologyEngineBuilder.build();
        terasologyEngine.initialize();
        registerCurrentDirectoryIfModule(terasologyEngine);

        engines.add(terasologyEngine);
        return terasologyEngine;
    }

    /**
     * In standalone module environments (i.e. Jenkins CI builds) the CWD is the module under test. When it uses MTE it very likely needs to
     * load itself as a module, but it won't be loadable from the typical path such as ./modules. This means that modules using MTE would
     * always fail CI tests due to failing to load themselves.
     * <p>
     * For these cases we try to load the CWD (via the installPath) as a module and put it in the global module registry.
     * <p>
     * This process is based on how ModuleManagerImpl uses ModulePathScanner to scan for available modules.
     */
    protected void registerCurrentDirectoryIfModule(TerasologyEngine terasologyEngine) {
        Path installPath = PathManager.getInstance().getInstallPath();
        ModuleManager moduleManager = terasologyEngine.getFromEngineContext(ModuleManager.class);
        ModuleRegistry registry = moduleManager.getRegistry();
        ModuleMetadataJsonAdapter metadataReader = moduleManager.getModuleMetadataReader();
        moduleManager.getModuleFactory().getModuleMetadataLoaderMap()
                .put(TerasologyConstants.MODULE_INFO_FILENAME.toString(), metadataReader);


        try {
            Module module = moduleManager.getModuleFactory().createModule(installPath.toFile());
            if (module != null) {
                registry.add(module);
                logger.info("Added install path as module: {}", installPath);
            } else {
                logger.info("Install path does not appear to be a module: {}", installPath);
            }
        } catch (IOException e) {
            logger.warn("Could not read install path as module at {}", installPath);
        }
    }

    protected void mockPathManager() {
        PathManager originalPathManager = PathManager.getInstance();
        if (org.mockito.Mockito.mockingDetails(originalPathManager).isMock()) {
            pathManager = originalPathManager;
        } else {
            pathManager = Mockito.spy(originalPathManager);
        }
        Mockito.when(pathManager.getModulePaths()).thenReturn(Collections.emptyList());
        pathManagerCleaner = new PathManagerProvider.Cleaner(originalPathManager, pathManager);
        PathManagerProvider.setPathManager(pathManager);
    }

    TerasologyEngine createHost() throws IOException {
        TerasologyEngine terasologyEngine = createHeadlessEngine();
        terasologyEngine.getFromEngineContext(SystemConfig.class).writeSaveGamesEnabled.set(false);
        terasologyEngine.subscribeToStateChange(new HeadlessStateChangeListener(terasologyEngine));
        terasologyEngine.changeState(new TestingStateHeadlessSetup(dependencies, worldGeneratorUri));

        doneLoading = false;
        terasologyEngine.subscribeToStateChange(() -> {
            GameState newState = terasologyEngine.getState();
            logger.debug("New engine state is {}", newState);
            if (newState instanceof StateIngame) {
                hostContext = newState.getContext();
                if (hostContext == null) {
                    logger.warn("hostContext is NULL in engine state {}", newState);
                }
                doneLoading = true;
            } else if (newState instanceof StateLoading) {
                CoreRegistry.put(GameEngine.class, terasologyEngine);
            }
        });

        boolean keepTicking;
        while (!doneLoading) {
            keepTicking = terasologyEngine.tick();
            if (!keepTicking) {
                throw new RuntimeException(String.format(
                        "Engine stopped ticking before we got in game. Current state: %s",
                        terasologyEngine.getState()
                ));
            }
        }
        return terasologyEngine;
    }

    /**
     * Joins {@code client} to the local host and runs the engines until it reaches
     * {@link StateIngame in-game}.
     *
     * @param client the client engine to connect to the local host
     * @param mainLoop used to tick the engines while the join completes
     * @throws RuntimeException if the client does not reach in-game before the wait times out. The
     *         message reports the client's state at that point and the {@link JoinStatus}, which
     *         distinguishes a join the host refused from one that was merely slow.
     */
    void connectToHost(TerasologyEngine client, MainLoop mainLoop) {
        Context clientContext = client.createChildContext();
        clientContext.put(Config.class, client.getFromEngineContext(Config.class));
        CoreRegistry.setContext(clientContext);
        JoinStatus joinStatus = null;
        try {
            joinStatus = clientContext.get(NetworkSystem.class).join("localhost", 25777);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while joining: ", e);
        }

        client.changeState(new StateLoading(joinStatus));
        clientContext.put(GameEngine.class, client);

        // TODO: subscribe to state change and return an asynchronous result
        //     so that we don't need to pass mainLoop to here.
        try {
            mainLoop.awaitUntil("client to finish joining and reach the in-game state",
                    () -> client.getState() instanceof StateIngame);
        } catch (AssertionError | UncheckedTimeoutException e) {
            // Both timeouts have to be caught here. awaitUntil throws AssertionError when the game-time
            // limit is reached, but MainLoop throws UncheckedTimeoutException when the real-time safety
            // timeout is hit first - and for a stuck join that is the likelier of the two, since an
            // engine that is not progressing may not advance game time at all. Letting that one through
            // would lose the diagnostics in exactly the case they are most needed.
            throw new RuntimeException(describeJoinFailure(client, joinStatus), e);
        }
    }

    /**
     * Explain why a client never reached in-game.
     * <p>
     * The wait result used to be discarded entirely, so a client that never connected was reported as
     * a success and the caller carried on with an engine that was not in-game - the failure surfaced
     * later, somewhere unrelated. {@link JoinStatus} knows whether the host refused the join, where it
     * stalled and why, and was going unused.
     */
    private static String describeJoinFailure(TerasologyEngine client, JoinStatus joinStatus) {
        // Read the state once: the engine is still running while we build this message, so calling
        // getState() twice could report one state and describe another, or NPE on the second call.
        GameState state = client.getState();
        StringBuilder message = new StringBuilder("Could not connect client ").append(client)
                .append(" to local host. Client state when we gave up: ")
                .append(state == null ? "none" : state.getClass().getSimpleName());

        if (joinStatus == null) {
            message.append("; the join was interrupted before it reported any status");
            return message.toString();
        }

        message.append("; join status ").append(joinStatus.getStatus())
                .append(" during '").append(joinStatus.getCurrentActivity())
                .append("' (").append(Math.round(joinStatus.getCurrentActivityProgress() * 100)).append("%)");

        String error = joinStatus.getErrorMessage();
        if (error != null && !error.isEmpty()) {
            message.append("; error: ").append(error);
        }
        return message.toString();
    }
}
