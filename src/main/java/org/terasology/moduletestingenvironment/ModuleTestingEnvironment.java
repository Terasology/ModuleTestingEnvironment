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

import com.google.api.client.util.Lists;
import com.google.common.collect.Sets;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.nio.file.ShrinkWrapFileSystems;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.config.Config;
import org.terasology.context.Context;
import org.terasology.engine.GameEngine;
import org.terasology.engine.StateChangeSubscriber;
import org.terasology.engine.TerasologyEngine;
import org.terasology.engine.TerasologyEngineBuilder;
import org.terasology.engine.modes.StateIngame;
import org.terasology.engine.modes.StateLoading;
import org.terasology.engine.modes.StateMainMenu;
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
import org.terasology.network.JoinStatus;
import org.terasology.network.NetworkSystem;
import org.terasology.registry.CoreRegistry;
import org.terasology.utilities.LWJGLHelper;
import org.terasology.world.RelevanceRegionComponent;
import org.terasology.world.WorldProvider;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Base class for tests involving full {@link TerasologyEngine} instances. View the tests included in this module for
 * simple usage examples
 * <p>
 * <h2>Introduction</h2> This class will create a new host engine for each @Test method. The in-game {@link Context} for
 * this engine can be accessed via {@link #getHostContext()}. The result of this getter is equivalent to the
 * CoreRegistry available to module code at runtime. However, it is very important that you do not use CoreRegistry in
 * your test code, as this is manipulated by the test environment to allow multiple instances of the engine to
 * peacefully coexist. You should always use the returned context reference to manipulate or inspect the CoreRegistry of
 * a given engine instance.
 * <p>
 * <h2>Client Engine Instances</h2> Client instances can be easily created via {@link #createClient()} which returns the
 * in-game context of the created engine instance. When this method returns, the client will be in the {@link
 * StateIngame} state and connected to the host. Currently all engine instances are headless, though it is possible to
 * use headed engines in the future.
 * <p>
 * Engines can be run while a condition is true via {@link #runWhile(Supplier)} <br>{@code runWhile(()-> true);}
 * <p>
 * or conversely run until a condition is true via {@link #runUntil(Supplier)} <br>{@code runUntil(()-> false);}
 * <p>
 * <h2>Specifying Dependencies</h2> By default the environment will load only the engine itself. In order to load your
 * own module code, you must override {@link #getDependencies()} in your test subclass.
 * <pre>
 * {@literal
 * public Set<String> getDependencies() {
 *     return Sets.newHashSet("engine", "ModuleTestingEnvironment");
 * }
 * }
 * </pre>
 * <h2>Specifying World Generator</h2> By default the environment will use a dummy world generator which creates
 * nothing but air. To specify a more useful world generator you must override {@link #getWorldGeneratorUri()} in your
 * test subclass.
 * <pre>
 * {@literal
 * public String getWorldGeneratorUri() {
 *     return "moduletestingenvironment:dummy";
 * }
 * }
 * </pre>
 */

public class ModuleTestingEnvironment {
    private static final Logger logger = LoggerFactory.getLogger(ModuleTestingEnvironment.class);
    private boolean doneLoading;
    private TerasologyEngine host;
    private Context hostContext;
    private List<TerasologyEngine> engines = Lists.newArrayList();

    @Before
    public void setup() throws Exception {
        host = createHost();
        CoreRegistry.put(GameEngine.class, host);
    }

    @After
    public void tearDown() throws Exception {
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
        return Sets.newHashSet("engine");
    }

    /**
     * Override this to change which world generator to use. Defaults to a dummy generator that leaves all blocks as air
     *
     * @return the uri of the desired world generator
     */
    public String getWorldGeneratorUri() {
        return "moduletestingenvironment:dummy";
    }

    /**
     * Creates a dummy entity with RelevanceRegion component to force a chunk's generation and availability.
     * Blocks while waiting for the chunk to become loaded
     *
     * @param blockPos the block position of the dummy entity. Only the chunk containing this position will be
     *                 available
     */
    protected void forceAndWaitForGeneration(Vector3i blockPos) {
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
    protected void runUntil(Supplier<Boolean> f) {
        runWhile(() -> !f.get());
    }

    /**
     * Runs tick() on the engine while f evaluates to true
     */
    protected void runWhile(Supplier<Boolean> f) {
        while (f.get()) {
            Thread.yield();
            for (TerasologyEngine terasologyEngine : engines) {
                terasologyEngine.tick();
            }
        }
    }

    /**
     * Creates a new client and connects it to the host
     * @return the created client's context object
     */
    protected Context createClient() {
        TerasologyEngine terasologyEngine = createHeadlessEngine();
        terasologyEngine.changeState(new StateMainMenu());
        connectToHost(terasologyEngine);
        return terasologyEngine.getState().getContext();
    }

    protected List<TerasologyEngine> getEngines() {
        return Lists.newArrayList(engines);
    }

    protected Context getHostContext() {
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
        try {
            // Cleverly uses an ephemeral java archive as an empty filesystem for the home path
            // this decouples the test environment from the contents of the local home directory
            final JavaArchive homeArchive = ShrinkWrap.create(JavaArchive.class);
            final FileSystem vfs = ShrinkWrapFileSystems.newFileSystem(homeArchive);
            PathManager.getInstance().useOverrideHomePath(vfs.getPath(""));
        } catch (Exception e) {
            logger.warn("Exception creating archive: {}", e);
            return null;
        }

        TerasologyEngine terasologyEngine = terasologyEngineBuilder.build();
        terasologyEngine.initialize();

        engines.add(terasologyEngine);
        return terasologyEngine;
    }

    private TerasologyEngine createHost() {
        TerasologyEngine terasologyEngine = createHeadlessEngine();
        terasologyEngine.getFromEngineContext(Config.class).getSystem().setWriteSaveGamesEnabled(false);
        terasologyEngine.subscribeToStateChange(new HeadlessStateChangeListener(terasologyEngine));
        terasologyEngine.changeState(new TestingStateHeadlessSetup(getDependencies(), getWorldGeneratorUri()));

        doneLoading = false;
        terasologyEngine.subscribeToStateChange(new StateChangeSubscriber() {
            @Override
            public void onStateChange() {
                if (terasologyEngine.getState() instanceof StateIngame) {
                    hostContext = terasologyEngine.getState().getContext();
                    doneLoading = true;
                } else if (terasologyEngine.getState() instanceof StateLoading) {
                    CoreRegistry.put(GameEngine.class, terasologyEngine);
                }
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
            logger.warn("Interrupted while joining: {}", e);
        }

        client.changeState(new StateLoading(joinStatus));
        CoreRegistry.put(GameEngine.class, client);

        runUntil(() -> client.getState() instanceof StateIngame);
    }
}
