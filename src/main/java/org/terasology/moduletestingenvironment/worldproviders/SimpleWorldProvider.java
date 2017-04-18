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
package org.terasology.moduletestingenvironment.worldproviders;

import com.google.api.client.util.Maps;
import org.terasology.context.Context;
import org.terasology.engine.SimpleUri;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.WorldChangeListener;
import org.terasology.world.WorldProvider;
import org.terasology.world.biomes.Biome;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.internal.GeneratingChunkProvider;
import org.terasology.world.internal.ChunkViewCore;
import org.terasology.world.internal.WorldInfo;
import org.terasology.world.internal.WorldProviderCore;
import org.terasology.world.internal.WorldProviderCoreImpl;
import org.terasology.world.liquid.LiquidData;
import org.terasology.world.time.WorldTime;

import java.util.Collection;
import java.util.Map;

/**
 * Provides a world with dirt at Y=0 with stone below that and air above
 */
public class SimpleWorldProvider implements WorldProvider {
    private BlockManager blockManager;
    private Context context;
    private Map<Vector3i, Block> blockMap = Maps.newHashMap();

    public SimpleWorldProvider(Context context) {
        this.context = context;
        blockManager = context.get(BlockManager.class);
    }

    @Override
    public EntityRef getWorldEntity() {
        return context.get(EntityManager.class).create();
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSeed() {
        return null;
    }

    @Override
    public WorldInfo getWorldInfo() {
        return null;
    }

    @Override
    public void processPropagation() {

    }

    @Override
    public void registerListener(WorldChangeListener listener) {

    }

    @Override
    public void unregisterListener(WorldChangeListener listener) {

    }

    @Override
    public ChunkViewCore getLocalView(Vector3i chunkPos) {
        return null;
    }

    @Override
    public ChunkViewCore getWorldViewAround(Vector3i chunk) {
        return null;
    }

    @Override
    public boolean isBlockRelevant(int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isRegionRelevant(Region3i region) {
        return true;
    }

    @Override
    public Block setBlock(Vector3i pos, Block type) {
        blockMap.put(pos, type);
        return type;
    }

    @Override
    public Biome setBiome(Vector3i pos, Biome biome) {
        return null;
    }

    @Override
    public Biome getBiome(Vector3i pos) {
        return null;
    }

    @Override
    public boolean setLiquid(int x, int y, int z, LiquidData newData, LiquidData oldData) {
        return false;
    }

    @Override
    public LiquidData getLiquid(int x, int y, int z) {
        return null;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        Vector3i pos = new Vector3i(x,y,z);
        if(blockMap.containsKey(pos)) {
            return blockMap.get(pos);
        }

        if(y <= 0) {
            return blockManager.getBlock("core:dirt");
        }

        return blockManager.getBlock("engine:air");
    }

    @Override
    public byte getLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public byte getSunlight(int x, int y, int z) {
        return 0;
    }

    @Override
    public byte getTotalLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public void dispose() {

    }

    @Override
    public WorldTime getTime() {
        return null;
    }

    @Override
    public Collection<Region3i> getRelevantRegions() {
        return null;
    }

    @Override
    public boolean isBlockRelevant(Vector3i pos) {
        return false;
    }

    @Override
    public boolean isBlockRelevant(Vector3f pos) {
        return false;
    }

    @Override
    public boolean setLiquid(Vector3i pos, LiquidData state, LiquidData oldState) {
        return false;
    }

    @Override
    public LiquidData getLiquid(Vector3i blockPos) {
        return null;
    }

    @Override
    public Block getBlock(Vector3f pos) {
        return getBlock((int) pos.x, (int) pos.y, (int) pos.z);
    }

    @Override
    public Block getBlock(Vector3i pos) {
        return getBlock((int) pos.x, (int) pos.y, (int) pos.z);
    }

    @Override
    public byte getLight(Vector3f pos) {
        return 0;
    }

    @Override
    public byte getSunlight(Vector3f pos) {
        return 0;
    }

    @Override
    public byte getTotalLight(Vector3f pos) {
        return 0;
    }

    @Override
    public byte getLight(Vector3i pos) {
        return 0;
    }

    @Override
    public byte getSunlight(Vector3i pos) {
        return 0;
    }

    @Override
    public byte getTotalLight(Vector3i pos) {
        return 0;
    }
}
