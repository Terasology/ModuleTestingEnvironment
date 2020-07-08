/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.moduletestingenvironment;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.config.Config;
import org.terasology.context.Context;
import org.terasology.engine.GameEngine;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.TerasologyEngine;
import org.terasology.engine.TerasologyEngineBuilder;
import org.terasology.engine.modes.StateIngame;
import org.terasology.engine.modes.StateLoading;
import org.terasology.engine.modes.StateMainMenu;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.paths.PathManager;
import org.terasology.engine.subsystem.EngineSubsystem;
import org.terasology.engine.subsystem.headless.HeadlessAudio;
import org.terasology.engine.subsystem.headless.HeadlessGraphics;
import org.terasology.engine.subsystem.headless.HeadlessInput;
import org.terasology.engine.subsystem.headless.HeadlessTimer;
import org.terasology.engine.subsystem.headless.mode.HeadlessStateChangeListener;
import org.terasology.engine.subsystem.lwjgl.LwjglAudio;
import org.terasology.engine.subsystem.lwjgl.LwjglGraphics;
import org.terasology.engine.subsystem.lwjgl.LwjglInput;
import org.terasology.engine.subsystem.lwjgl.LwjglTimer;
import org.terasology.engine.subsystem.openvr.OpenVRInput;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3i;
import org.terasology.module.Module;
import org.terasology.module.ModuleLoader;
import org.terasology.module.ModuleMetadataJsonAdapter;
import org.terasology.module.ModuleRegistry;
import org.terasology.network.JoinStatus;
import org.terasology.network.NetworkSystem;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.opengl.ScreenGrabber;
import org.terasology.world.RelevanceRegionComponent;
import org.terasology.world.WorldProvider;

import java.io.IOException;
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
 */
public class ModuleTestingEnvironment {
    private static final Logger logger = LoggerFactory.getLogger(ModuleTestingEnvironment.class);
    private Set<String> dependencies = Sets.newHashSet("engine");
    private String worldGeneratorUri = "moduletestingenvironment:dummy";
    private boolean doneLoading;
    private TerasologyEngine host;
    private Context hostContext;
    private List<TerasologyEngine> engines = Lists.newArrayList();

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

        // relevance distance has to be at least 2 to get adjacent chunks in the cache, or else our main chunk will never be accessible
        RelevanceRegionComponent relevanceRegionComponent = new RelevanceRegionComponent();
        relevanceRegionComponent.distance = new Vector3i(2, 2, 2);

        hostContext.get(EntityManager.class).create(locationComponent, relevanceRegionComponent).setAlwaysRelevant(true);

        runWhile(() -> hostContext.get(WorldProvider.class).getBlock(blockPos).getURI().toString().equalsIgnoreCase("engine:unloaded"));
    }

    /**
     * Runs tick() on the engine until f evaluates to true
     */
    public void runUntil(Supplier<Boolean> f) {
        runWhile(() -> !f.get());
    }

    /**
     * Runs tick() on the engine while f evaluates to true
     */
    public void runWhile(Supplier<Boolean> f) {
        while (f.get()) {
            Thread.yield();
            for (TerasologyEngine terasologyEngine : engines) {
                terasologyEngine.tick();
            }
        }
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
     * In standalone module environments (i.e. Jenkins CI builds) the CWD is the module under test. When it uses MTE
     * it very likely needs to load itself as a module, but it won't be loadable from the typical path such as
     * ./modules. This means that modules using MTE would always fail CI tests due to failing to load themselves.
     *
     * For these cases we try to load the CWD (via the installPath) as a module and put it in the global module
     * registry.
     *
     * This process is based on how ModuleManagerImpl uses ModulePathScanner to scan for available modules.
     *
     * @param terasologyEngine
     */
    private void registerCurrentDirectoryIfModule(TerasologyEngine terasologyEngine) {
        Path installPath = PathManager.getInstance().getInstallPath();
        ModuleManager moduleManager = terasologyEngine.getFromEngineContext(ModuleManager.class);
        ModuleRegistry registry = moduleManager.getRegistry();
        ModuleMetadataJsonAdapter metadataReader = moduleManager.getModuleMetadataReader();
        ModuleLoader moduleLoader = new ModuleLoader(metadataReader);
        moduleLoader.setModuleInfoPath(TerasologyConstants.MODULE_INFO_FILENAME);

        try {
            Module module = moduleLoader.load(installPath);
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
        terasologyEngine.subscribeToStateChange(new HeadlessStateChangeListener(terasologyEngine));
        terasologyEngine.changeState(new TestingStateHeadlessSetup(getDependencies(), getWorldGeneratorUri()));

        doneLoading = false;
        terasologyEngine.subscribeToStateChange(() -> {
            if (terasologyEngine.getState() instanceof org.terasology.engine.modes.StateIngame) {
                hostContext = terasologyEngine.getState().getContext();
                doneLoading = true;
            } else if (terasologyEngine.getState() instanceof org.terasology.engine.modes.StateLoading) {
                org.terasology.registry.CoreRegistry.put(org.terasology.engine.GameEngine.class, terasologyEngine);
            }
        });

        while (!doneLoading && terasologyEngine.tick()) { /* do nothing */ }
        return terasologyEngine;
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
