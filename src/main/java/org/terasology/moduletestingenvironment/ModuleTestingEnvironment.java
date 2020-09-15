// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.config.Config;
import org.terasology.engine.config.SystemConfig;
import org.terasology.engine.context.Context;
import org.terasology.engine.core.GameEngine;
import org.terasology.engine.core.TerasologyConstants;
import org.terasology.engine.core.TerasologyEngine;
import org.terasology.engine.core.TerasologyEngineBuilder;
import org.terasology.engine.core.Time;
import org.terasology.engine.core.modes.GameState;
import org.terasology.engine.core.modes.StateIngame;
import org.terasology.engine.core.modes.StateLoading;
import org.terasology.engine.core.modes.StateMainMenu;
import org.terasology.engine.core.module.ModuleManager;
import org.terasology.engine.core.paths.PathManager;
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
import org.terasology.engine.core.subsystem.openvr.OpenVRInput;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.network.JoinStatus;
import org.terasology.engine.network.NetworkSystem;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.rendering.opengl.ScreenGrabber;
import org.terasology.engine.utilities.ReflectionUtil;
import org.terasology.engine.world.RelevanceRegionComponent;
import org.terasology.engine.world.WorldProvider;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleFactory;
import org.terasology.gestalt.module.ModuleMetadata;
import org.terasology.gestalt.module.ModuleMetadataJsonAdapter;
import org.terasology.gestalt.module.ModuleRegistry;
import org.terasology.gestalt.naming.Name;
import org.terasology.math.geom.Vector3i;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.mockito.Mockito.mock;

/**
 * Base class for tests involving full {@link TerasologyEngine} instances. View the tests included in this module for
 * simple usage examples
 * <p>
 * <h2>Introduction</h2>
 * If test classes extend this class will create a new host engine for each {@code @Test} method. If the testing
 * environment is used by composition {@link #setup()} and {@link #tearDown()} need to be called explicitly. This can be
 * done once for the test class or for each test.
 * <p>
 * The in-game {@link Context} for this engine can be accessed via {@link #getHostContext()}. The result of this getter
 * is equivalent to the CoreRegistry available to module code at runtime. However, it is very important that you do not
 * use CoreRegistry in your test code, as this is manipulated by the test environment to allow multiple instances of the
 * engine to peacefully coexist. You should always use the returned context reference to manipulate or inspect the
 * CoreRegistry of a given engine instance.
 * <p>
 * <h2>Client Engine Instances</h2>
 * Client instances can be easily created via {@link #createClient()} which returns the in-game context of the created
 * engine instance. When this method returns, the client will be in the {@link StateIngame} state and connected to the
 * host. Currently all engine instances are headless, though it is possible to use headed engines in the future.
 * <p>
 * Engines can be run while a condition is true via {@link #runWhile(Supplier)} <br>{@code runWhile(()-> true);}
 * <p>
 * or conversely run until a condition is true via {@link #runUntil(Supplier)} <br>{@code runUntil(()-> false);}
 * <p>
 * <h2>Specifying Dependencies</h2>
 * By default the environment will load only the engine itself. In order to load your own module code, you must override
 * {@link #getDependencies()} in your test subclass.
 * <pre>
 * {@literal
 * public Set<String> getDependencies() {
 *     return Sets.newHashSet("engine", "ModuleTestingEnvironment");
 * }
 * }
 * </pre>
 * <h2>Specifying World Generator</h2>
 * By default the environment will use a dummy world generator which creates nothing but air. To specify a more useful
 * world generator you must override {@link #getWorldGeneratorUri()} in your test subclass.
 * <pre>
 * {@literal
 * public String getWorldGeneratorUri() {
 *     return "moduletestingenvironment:dummy";
 * }
 * }
 * </pre>
 * <p>
 * <h2>Reuse the MTE for Multiple Tests</h2>
 * To use the same engine for multiple tests the testing environment can be set up explicitly and shared between tests.
 * To configure module dependencies or the world generator an anonymous class may be used.
 * <pre>
 * {@code
 * private static ModuleTestingEnvironment context;
 *
 * @BeforeAll
 * public static void setup() throws Exception {
 *     context = new ModuleTestingEnvironment() {
 *     @Override
 *     public Set<String> getDependencies() {
 *         return Sets.newHashSet("ModuleTestingEnvironment");
 *     }
 *     };
 *     context.setup();
 * }
 *
 * @AfterAll
 * public static void tearDown() throws Exception {
 *     context.tearDown();
 * }
 *
 * @Test
 * public void someTest() {
 * 	   Context hostContext = context.getHostContext();
 *     EntityManager entityManager = hostContext.get(EntityManager.class);
 *     // ...
 * }
 * }
 * </pre>
 *
 * @deprecated Use the {@link MTEExtension} or {@link IsolatedMTEExtension} instead with JUnit5.
 */
@Deprecated
public class ModuleTestingEnvironment {
    @Deprecated
    public static final long DEFAULT_TIMEOUT = 30000;

    public static final long DEFAULT_SAFETY_TIMEOUT = 60000;
    public static final long DEFAULT_GAME_TIME_TIMEOUT = 30000;
    private static final Logger logger = LoggerFactory.getLogger(ModuleTestingEnvironment.class);
    private final List<TerasologyEngine> engines = Lists.newArrayList();
    private Set<String> dependencies = Sets.newHashSet("engine");
    private String worldGeneratorUri = "moduletestingenvironment:dummy";
    private boolean doneLoading;
    private TerasologyEngine host;
    private Context hostContext;
    private long safetyTimeoutMs = DEFAULT_SAFETY_TIMEOUT;

    /**
     * Set up and start the engine as configured via this environment.
     * <p>
     * Every instance should be shut down properly by calling {@link #tearDown()}.
     *
     * @throws Exception
     */
    @Before
    @BeforeEach
    public void setup() throws Exception {
        host = createHost();
        ScreenGrabber grabber = mock(ScreenGrabber.class);
        hostContext.put(ScreenGrabber.class, grabber);
        CoreRegistry.put(GameEngine.class, host);
    }

    /**
     * Shut down a previously started testing environment.
     * <p>
     * Used to properly shut down and clean up a testing environment set up and started with {@link #setup()}.
     */
    @After
    @AfterEach
    public void tearDown() {
        engines.forEach(TerasologyEngine::shutdown);
        engines.forEach(TerasologyEngine::cleanup);
        engines.clear();
        host = null;
        hostContext = null;
    }

    /**
     * Override this to change which modules must be loaded for the environment
     *
     * @return The set of module names to load
     */
    public Set<String> getDependencies() {
        return dependencies;
    }

    /**
     * Setting dependencies for using by {@link ModuleTestingEnvironment}.
     *
     * @param dependencies the set of module names to load
     * @throws IllegalStateException if you try'd setWorldGeneratorUrl after {@link
     *         ModuleTestingEnvironment#setup()}
     */
    void setDependencies(Set<String> dependencies) {
        Preconditions.checkState(host == null, "You cannot set Dependencies after setup");
        this.dependencies = dependencies;
    }

    /**
     * Override this to change which world generator to use. Defaults to a dummy generator that leaves all blocks as
     * air
     *
     * @return the uri of the desired world generator
     */
    public String getWorldGeneratorUri() {
        return worldGeneratorUri;
    }

    /**
     * Setting world generator for using by {@link ModuleTestingEnvironment}.
     *
     * @param worldGeneratorUri the uri of desired world generator
     * @throws IllegalStateException if you try'd setWorldGeneratorUrl after {@link
     *         ModuleTestingEnvironment#setup()}
     */
    void setWorldGeneratorUri(String worldGeneratorUri) {
        Preconditions.checkState(host == null, "You cannot set Dependencies after setup");
        this.worldGeneratorUri = worldGeneratorUri;
    }


    /**
     * Creates a dummy entity with RelevanceRegion component to force a chunk's generation and availability. Blocks
     * while waiting for the chunk to become loaded
     *
     * @param blockPos the block position of the dummy entity. Only the chunk containing this position will be
     *         available
     */
    public void forceAndWaitForGeneration(Vector3i blockPos) {
        // we need to add an entity with RegionRelevance in order to get a chunk generated
        LocationComponent locationComponent = new LocationComponent();
        locationComponent.setWorldPosition(blockPos.toVector3f());

        // relevance distance has to be at least 2 to get adjacent chunks in the cache, or else our main chunk will
        // never be accessible
        RelevanceRegionComponent relevanceRegionComponent = new RelevanceRegionComponent();
        relevanceRegionComponent.distance = new Vector3i(2, 2, 2);

        hostContext.get(EntityManager.class).create(locationComponent, relevanceRegionComponent).setAlwaysRelevant(true);

        runWhile(() -> hostContext.get(WorldProvider.class).getBlock(blockPos).getURI().toString().equalsIgnoreCase(
                "engine:unloaded"));
    }

    /**
     * Runs tick() on the engine until f evaluates to true or DEFAULT_GAME_TIME_TIMEOUT milliseconds have passed in game
     * time
     */
    public void runUntil(Supplier<Boolean> f) {
        runWhile(() -> !f.get());
    }

    /**
     * Runs tick() on the engine until f evaluates to true or gameTimeTimeoutMs has passed in game time
     *
     * @return true if execution timed out
     */
    public boolean runUntil(long gameTimeTimeoutMs, Supplier<Boolean> f) {
        return runWhile(gameTimeTimeoutMs, () -> !f.get());
    }

    /**
     * Runs tick() on the engine while f evaluates to true or until DEFAULT_GAME_TIME_TIMEOUT milliseconds have passed
     */
    public void runWhile(Supplier<Boolean> f) {
        runWhile(DEFAULT_GAME_TIME_TIMEOUT, f);
    }

    /**
     * Runs tick() on the engine while f evaluates to true or until gameTimeTimeoutMs has passed in game time.
     *
     * @return true if execution timed out
     */
    public boolean runWhile(long gameTimeTimeoutMs, Supplier<Boolean> f) {
        boolean timedOut = false;
        Time hostTime = getHostContext().get(Time.class);
        long startRealTime = System.currentTimeMillis();
        long startGameTime = hostTime.getGameTimeInMs();

        while (f.get() && !timedOut) {
            Thread.yield();
            for (TerasologyEngine terasologyEngine : engines) {
                terasologyEngine.tick();
            }

            // handle safety timeout
            if (System.currentTimeMillis() - startRealTime > safetyTimeoutMs) {
                timedOut = true;
                Assertions.assertTrue(false, "MTE Safety timeout exceeded. See setSafetyTimeoutMs()");
            }

            // handle game time timeout
            if (hostTime.getGameTimeInMs() - startGameTime > gameTimeTimeoutMs) {
                timedOut = true;
            }
        }

        return timedOut;
    }

    /**
     * Creates a new client and connects it to the host
     *
     * @return the created client's context object
     */
    public Context createClient() {
        TerasologyEngine terasologyEngine = createHeadlessEngine();
        terasologyEngine.changeState(new StateMainMenu());
        connectToHost(terasologyEngine);
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
     * The host context will be null if the testing environment has not been set up via {@link
     * ModuleTestingEnvironment#setup()} beforehand.
     *
     * @return the engine's host context, or null if not set up yet
     */
    public Context getHostContext() {
        return hostContext;
    }


    /**
     * @return the current safety timeout
     */
    public long getSafetyTimeoutMs() {
        return safetyTimeoutMs;
    }

    /**
     * Sets the safety timeout (default 30000ms). The safety timeout applies to `runWhile` and related helpers, and
     * stops execution when the specified number of real time milliseconds has passsed. Note that this is different from
     * the timeout parameter of those methods, which is specified in game time.
     * <p>
     * When a single run* helper invocation exceeds the safety timeout, MTE asserts false to explicitly fail the test.
     * <p>
     * The safety timeout exists to prevent indefinite execution in Jenkins or long IDE test runs, and should be
     * adjusted as needed so that tests pass reliably in all environments.
     *
     * @param safetyTimeoutMs
     */
    public void setSafetyTimeoutMs(long safetyTimeoutMs) {
        this.safetyTimeoutMs = safetyTimeoutMs;
    }

    private TerasologyEngine createHeadlessEngine() {
        TerasologyEngineBuilder terasologyEngineBuilder = new TerasologyEngineBuilder();
        terasologyEngineBuilder
                .add(new HeadlessGraphics())
                .add(new HeadlessTimer())
                .add(new HeadlessAudio())
                .add(new HeadlessInput());

        return createEngine(terasologyEngineBuilder);
    }

    private TerasologyEngine createHeadedEngine() {
        EngineSubsystem audio = new LwjglAudio();
        TerasologyEngineBuilder terasologyEngineBuilder = new TerasologyEngineBuilder()
                .add(audio)
                .add(new LwjglGraphics())
                .add(new LwjglTimer())
                .add(new LwjglInput())
                .add(new OpenVRInput());

        return createEngine(terasologyEngineBuilder);
    }

    private TerasologyEngine createEngine(TerasologyEngineBuilder terasologyEngineBuilder) {
        // create temporary home paths so the MTE engines don't overwrite config/save files in your real home path
        try {
            Path path = Files.createTempDirectory("terasology-mte-engine");
            PathManager.getInstance().useOverrideHomePath(path);
            logger.info("Created temporary engine home path");

            // JVM will delete these on normal termination but not exceptions.
            path.toFile().deleteOnExit();
        } catch (Exception e) {
            logger.warn("Exception creating temporary home path for engine: ", e);
            return null;
        }

        TerasologyEngine terasologyEngine = terasologyEngineBuilder.build();
        terasologyEngine.initialize();
        registerCurrentDirectoryIfModule(terasologyEngine);

        engines.add(terasologyEngine);
        return terasologyEngine;
    }

    /**
     * In standalone module environments (i.e. Jenkins CI builds) the CWD is the module under test. When it uses MTE it
     * very likely needs to load itself as a module, but it won't be loadable from the typical path such as ./modules.
     * This means that modules using MTE would always fail CI tests due to failing to load themselves.
     * <p>
     * For these cases we try to load the CWD (via the installPath) as a module and put it in the global module
     * registry.
     * <p>
     * This process is based on how ModuleManagerImpl uses ModulePathScanner to scan for available modules.
     *
     * @param terasologyEngine
     */
    private void registerCurrentDirectoryIfModule(TerasologyEngine terasologyEngine) {
        Path installPath = PathManager.getInstance().getInstallPath();
        ModuleManager moduleManager = terasologyEngine.getFromEngineContext(ModuleManager.class);
        ModuleRegistry registry = moduleManager.getRegistry();
        ModuleMetadataJsonAdapter metadataReader = moduleManager.getModuleMetadataReader();
        ModuleFactory moduleFactory = new ModuleFactory();
        moduleFactory.getModuleMetadataLoaderMap().put(TerasologyConstants.MODULE_INFO_FILENAME.toString(),
                metadataReader);


        try {
            Module module = moduleFactory.createModule(installPath.toFile());
            if (module != null) {
                registry.add(module);
                logger.info("Added install path as module: {}", installPath);
            } else {
                logger.info("Install path does not appear to be a module: {}", installPath);
            }
        } catch (IOException e) {
            logger.warn("Could not read install path as module at " + installPath);
        }
    }

    private TerasologyEngine createHost() {
        TerasologyEngine terasologyEngine = createHeadlessEngine();
        terasologyEngine.getFromEngineContext(Config.class).getSystem().setWriteSaveGamesEnabled(false);
        replaceModuleTestingEnvironmentModuleWithPackageModule(terasologyEngine);
        terasologyEngine.subscribeToStateChange(new HeadlessStateChangeListener(terasologyEngine));
        terasologyEngine.changeState(new TestingStateHeadlessSetup(getDependencies(), getWorldGeneratorUri()));

        doneLoading = false;
        terasologyEngine.subscribeToStateChange(() -> {
            if (terasologyEngine.getState() instanceof org.terasology.engine.core.modes.StateIngame) {
                hostContext = terasologyEngine.getState().getContext();
                doneLoading = true;
            } else if (terasologyEngine.getState() instanceof org.terasology.engine.core.modes.StateLoading) {
                org.terasology.engine.registry.CoreRegistry.put(org.terasology.engine.core.GameEngine.class,
                        terasologyEngine);
            }
        });

        while (!doneLoading && terasologyEngine.tick()) { /* do nothing */ }
        Assertions.assertTrue(doneLoading, () -> {
            GameState gameState = terasologyEngine.getState();

            if (gameState instanceof StateMainMenu) {
                String messageOnLoad = (String) ReflectionUtil.readField(gameState, "messageOnLoad");
                if (messageOnLoad != null && !messageOnLoad.isEmpty()) {
                    return "Failed to load ModuleTestingEnvironment: " + messageOnLoad;
                }
            }
            return "Failed to load ModuleTestingEnvironment, engine stuck on state " + gameState.getClass().getSimpleName();
        });

        return terasologyEngine;
    }

    /
    private void replaceModuleTestingEnvironmentModuleWithPackageModule(TerasologyEngine terasologyEngine) {
        ModuleManager moduleManager = terasologyEngine.getFromEngineContext(ModuleManager.class);
        moduleManager.getRegistry().removeIf((m)-> m.getId().equals(new Name("ModuleTestingEnvironment")));
        ModuleMetadataJsonAdapter mmja= new ModuleMetadataJsonAdapter();
        ModuleMetadata metadata=null;
        try (InputStream is = ModuleTestingEnvironment.class.getResource("/module.txt").openStream()){
            metadata = mmja.read(new InputStreamReader(is));
        } catch (IOException e) {
           logger.error("Cannot read module.txt for ModuleTestingEnvironment");
        }
        moduleManager.getRegistry().add(moduleManager.getModuleFactory().createPackageModule(metadata,"org.terasology.moduletestingenvironment"));
    }

    private void connectToHost(TerasologyEngine client) {
        CoreRegistry.put(Config.class, client.getFromEngineContext(Config.class));
        JoinStatus joinStatus = null;
        try {
            joinStatus = client.getFromEngineContext(NetworkSystem.class).join("localhost", 25777);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while joining: ", e);
        }

        client.changeState(new StateLoading(joinStatus));
        CoreRegistry.put(GameEngine.class, client);

        runUntil(() -> client.getState() instanceof StateIngame);
    }
}
