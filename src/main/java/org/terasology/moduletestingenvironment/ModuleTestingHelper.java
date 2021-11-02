// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.moduletestingenvironment;

import com.google.common.util.concurrent.ListenableFuture;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.terasology.engine.context.Context;
import org.terasology.engine.core.TerasologyEngine;
import org.terasology.engine.core.modes.StateIngame;
import org.terasology.engine.world.block.BlockRegion;
import org.terasology.engine.world.block.BlockRegionc;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Base class for tests involving full {@link TerasologyEngine} instances. View the tests included in this module for
 * simple usage examples.
 *
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
 *
 * <h2>Client Engine Instances</h2>
 * Client instances can be easily created via {@link #createClient()} which returns the in-game context of the created
 * engine instance. When this method returns, the client will be in the {@link StateIngame} state and connected to the
 * host. Currently all engine instances are headless, though it is possible to use headed engines in the future.
 * <p>
 * Engines can be run while a condition is true via {@link #runWhile(Supplier)} <br>{@code runWhile(()-> true);}
 * <p>
 * or conversely run until a condition is true via {@link #runUntil(Supplier)} <br>{@code runUntil(()-> false);}
 *
 * <h2>Specifying Dependencies</h2>
 * By default the environment will load only the engine itself. FIXME
 *
 * <h2>Specifying World Generator</h2>
 * By default the environment will use a dummy world generator which creates nothing but air. To specify a more useful
 * world generator you must FIXME
 *
 * <h2>Reuse the MTE for Multiple Tests</h2>
 * To use the same engine for multiple tests the testing environment can be set up explicitly and shared between tests.
 * To configure module dependencies or the world generator an anonymous class may be used.
 * <pre>
 * private static ModuleTestingHelper context;
 *
 * &#64;BeforeAll
 * public static void setup() throws Exception {
 *     context = new ModuleTestingHelper() {
 *     &#64;Override
 *     public Set&lt;String&gt; getDependencies() {
 *         return Sets.newHashSet("ModuleTestingHelper");
 *     }
 *     };
 *     context.setup();
 * }
 *
 * &#64;AfterAll
 * public static void tearDown() throws Exception {
 *     context.tearDown();
 * }
 *
 * &#64;Test
 * public void someTest() {
 *     Context hostContext = context.getHostContext();
 *     EntityManager entityManager = hostContext.get(EntityManager.class);
 *     // ...
 * }
 * </pre>
 */
public class ModuleTestingHelper implements ModuleTestingEnvironment {

    final Engines engines;
    final MainLoop mainLoop;

    protected ModuleTestingHelper(Set<String> dependencies, String worldGeneratorUri) {
        engines = new Engines(dependencies, worldGeneratorUri);
        mainLoop = new MainLoop(engines);
    }

    /**
     * Set up and start the engine as configured via this environment.
     * <p>
     * Every instance should be shut down properly by calling {@link #tearDown()}.
     */
    protected void setup() {
        engines.setup();
    }

    /**
     * Shut down a previously started testing environment.
     * <p>
     * Used to properly shut down and clean up a testing environment set up and started with {@link #setup()}.
     */
    protected void tearDown() {
        engines.tearDown();
    }

    @Override
    public void forceAndWaitForGeneration(Vector3ic blockPos) {
        mainLoop.forceAndWaitForGeneration(blockPos);
    }

    @Override
    public ListenableFuture<ChunkRegionFuture> makeBlocksRelevant(BlockRegionc blocks) {
        return mainLoop.makeBlocksRelevant(blocks);
    }

    @Override
    public ListenableFuture<ChunkRegionFuture> makeChunksRelevant(BlockRegion chunks) {
        return mainLoop.makeChunksRelevant(chunks);
    }

    @Override
    public ListenableFuture<ChunkRegionFuture> makeChunksRelevant(BlockRegion chunks, Vector3fc centerBlock) {
        return mainLoop.makeChunksRelevant(chunks, centerBlock);
    }

    @Override
    public <T> T runUntil(ListenableFuture<T> future) {
        return mainLoop.runUntil(future);
    }

    @Override
    public boolean runUntil(Supplier<Boolean> f) {
        return mainLoop.runUntil(f);
    }

    @Override
    public boolean runUntil(long gameTimeTimeoutMs, Supplier<Boolean> f) {
        return mainLoop.runUntil(gameTimeTimeoutMs, f);
    }

    @Override
    public boolean runWhile(Supplier<Boolean> f) {
        return mainLoop.runWhile(f);
    }

    @Override
    public boolean runWhile(long gameTimeTimeoutMs, Supplier<Boolean> f) {
        return mainLoop.runWhile(gameTimeTimeoutMs, f);
    }

    @Override
    public Context createClient() throws IOException {
        return engines.createClient(mainLoop);
    }

    @Override
    public List<TerasologyEngine> getEngines() {
        return engines.getEngines();
    }

    @Override
    public Context getHostContext() {
        return engines.getHostContext();
    }

    @Override
    public long getSafetyTimeoutMs() {
        return mainLoop.getSafetyTimeoutMs();
    }

    @Override
    public void setSafetyTimeoutMs(long safetyTimeoutMs) {
        mainLoop.setSafetyTimeoutMs(safetyTimeoutMs);
    }
}
