// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.joml.Matrix4f;
import org.joml.RoundingMode;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.engine.core.TerasologyEngine;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.BlockRegion;
import org.terasology.engine.world.block.BlockRegionc;
import org.terasology.engine.world.chunks.Chunks;
import org.terasology.engine.world.chunks.localChunkProvider.RelevanceSystem;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Methods to run the main loop of the game.
 * <p>
 * {@link MTEExtension} provides tests with a game engine, configured with a module environment
 * and a world. The engine is ready by the time a test method is executed, but does not <em>run</em>
 * until you use one of these methods.
 * <p>
 * If there are multiple engines (a host and one or more clients), they will tick in a round-robin fashion.
 */
public class MainLoop {
    long safetyTimeoutMs = ModuleTestingEnvironment.DEFAULT_SAFETY_TIMEOUT;

    private final Engines engines;

    public MainLoop(Engines engines) {
        this.engines = engines;
    }

    /**
     * Creates a dummy entity with RelevanceRegion component to force a chunk's generation and availability. Blocks while waiting for the
     * chunk to become loaded
     *
     * @param blockPos the block position of the dummy entity. Only the chunk containing this position will be available
     */
    public void forceAndWaitForGeneration(Vector3ic blockPos) {
        WorldProvider worldProvider = engines.getHostContext().get(WorldProvider.class);
        if (worldProvider.isBlockRelevant(blockPos)) {
            return;
        }

        ListenableFuture<ChunkRegionFuture> chunkRegion = makeBlocksRelevant(new BlockRegion(blockPos));
        runUntil(chunkRegion);
    }

    /**
     * @param blocks blocks to mark as relevant
     * @return relevant chunks
     */
    public ListenableFuture<ChunkRegionFuture> makeBlocksRelevant(BlockRegionc blocks) {
        BlockRegion desiredChunkRegion = Chunks.toChunkRegion(new BlockRegion(blocks));
        return makeChunksRelevant(desiredChunkRegion, blocks.center(new Vector3f()));
    }

    @SuppressWarnings("unused")
    public ListenableFuture<ChunkRegionFuture> makeChunksRelevant(BlockRegion chunks) {
        // Pick a central point (in block coordinates).
        Vector3f centerPoint = chunkRegionToNewBlockRegion(chunks).center(new Vector3f());

        return makeChunksRelevant(chunks, centerPoint);
    }

    public ListenableFuture<ChunkRegionFuture> makeChunksRelevant(BlockRegion chunks, Vector3fc centerBlock) {
        Preconditions.checkArgument(chunks.contains(Chunks.toChunkPos(new Vector3i(centerBlock, RoundingMode.FLOOR))),
                "centerBlock should %s be within the region %s",
                centerBlock, chunkRegionToNewBlockRegion(chunks));
        Vector3i desiredSize = chunks.getSize(new Vector3i());

        EntityManager entityManager = Verify.verifyNotNull(engines.getHostContext().get(EntityManager.class));
        RelevanceSystem relevanceSystem = Verify.verifyNotNull(engines.getHostContext().get(RelevanceSystem.class));
        ChunkRegionFuture listener = ChunkRegionFuture.create(entityManager, relevanceSystem, centerBlock, desiredSize);
        return listener.getFuture();
    }

    BlockRegionc chunkRegionToNewBlockRegion(BlockRegionc chunks) {
        BlockRegion blocks = new BlockRegion(chunks);
        return blocks.transform(new Matrix4f().scaling(new Vector3f(Chunks.CHUNK_SIZE)));
    }

    public <T> T runUntil(ListenableFuture<T> future) {
        boolean timedOut = runUntil(future::isDone);
        if (timedOut) {
            // TODO: if runUntil returns timedOut but does not throw an exception, it
            //     means it hit DEFAULT_GAME_TIME_TIMEOUT but not SAFETY_TIMEOUT, and
            //     that's a weird interface due for a revision.
            future.cancel(true);  // let it know we no longer expect results
            throw new UncheckedTimeoutException("No result within default timeout.");
        }
        try {
            return future.get(0, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for " + future, e);
        } catch (TimeoutException e) {
            throw new UncheckedTimeoutException(
                    "Checked isDone before calling get, so this shouldn't happen.", e);
        }
    }

    /**
     * Runs tick() on the engine until f evaluates to true or DEFAULT_GAME_TIME_TIMEOUT milliseconds have passed in game time
     *
     * @return true if execution timed out
     */
    public boolean runUntil(Supplier<Boolean> f) {
        return runWhile(() -> !f.get());
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
     *
     * @return true if execution timed out
     */
    public boolean runWhile(Supplier<Boolean> f) {
        return runWhile(ModuleTestingEnvironment.DEFAULT_GAME_TIME_TIMEOUT, f);
    }

    /**
     * Runs tick() on the engine while f evaluates to true or until gameTimeTimeoutMs has passed in game time.
     *
     * @return true if execution timed out
     */
    public boolean runWhile(long gameTimeTimeoutMs, Supplier<Boolean> f) {
        boolean timedOut = false;
        Time hostTime = engines.getHostContext().get(Time.class);
        long startRealTime = System.currentTimeMillis();
        long startGameTime = hostTime.getGameTimeInMs();

        while (f.get() && !timedOut) {
            Thread.yield();
            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeException(String.format("Thread %s interrupted while waiting for %s.",
                        Thread.currentThread(), f));
            }
            for (TerasologyEngine terasologyEngine : engines.getEngines()) {
                boolean keepRunning = terasologyEngine.tick();
                if (!keepRunning && terasologyEngine == engines.host) {
                    throw new RuntimeException("Host has shut down: " + engines.host.getStatus());
                }
            }

            // handle safety timeout
            if (System.currentTimeMillis() - startRealTime > safetyTimeoutMs) {
                timedOut = true;
                // If we've passed the _safety_ timeout, throw an exception.
                throw new UncheckedTimeoutException("MTE Safety timeout exceeded. See setSafetyTimeoutMs()");
            }

            // handle game time timeout
            if (hostTime.getGameTimeInMs() - startGameTime > gameTimeTimeoutMs) {
                // If we've passed the user-specified timeout but are still under the
                // safety threshold, set timed-out status without throwing.
                timedOut = true;
            }
        }

        return timedOut;
    }

    /**
     * @return the current safety timeout
     */
    public long getSafetyTimeoutMs() {
        return safetyTimeoutMs;
    }

    /**
     * Sets the safety timeout (default 30s).
     *
     * @param safetyTimeoutMs The safety timeout applies to {@link #runWhile runWhile} and related helpers, and stops execution when
     *         the specified number of real time milliseconds has passed. Note that this is different from the timeout parameter of those
     *         methods, which is specified in game time.
     *         <p>
     *         When a single {@code run*} helper invocation exceeds the safety timeout, MTE asserts false to explicitly fail the test.
     *         <p>
     *         The safety timeout exists to prevent indefinite execution in Jenkins or long IDE test runs, and should be adjusted as needed
     *         so that tests pass reliably in all environments.
     */
    public void setSafetyTimeoutMs(long safetyTimeoutMs) {
        this.safetyTimeoutMs = safetyTimeoutMs;
    }
}
