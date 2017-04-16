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
package org.terasology;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.nio.file.ShrinkWrapFileSystems;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.HeadlessEnvironment;
import org.terasology.TerasologyTestingEnvironment;
import org.terasology.assets.management.AssetManager;
import org.terasology.audio.AudioManager;
import org.terasology.config.Config;
import org.terasology.context.Context;
import org.terasology.engine.ComponentSystemManager;
import org.terasology.engine.EngineTime;
import org.terasology.engine.Time;
import org.terasology.engine.bootstrap.EntitySystemSetupUtil;
import org.terasology.engine.modes.loadProcesses.LoadPrefabs;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.paths.PathManager;
import org.terasology.engine.subsystem.DisplayDevice;
import org.terasology.engine.subsystem.headless.device.HeadlessDisplayDevice;
import org.terasology.engine.subsystem.headless.renderer.HeadlessCanvasRenderer;
import org.terasology.entitySystem.entity.internal.EngineEntityManager;
import org.terasology.game.Game;
import org.terasology.logic.console.Console;
import org.terasology.logic.console.ConsoleImpl;
import org.terasology.naming.Name;
import org.terasology.network.NetworkMode;
import org.terasology.network.NetworkSystem;
import org.terasology.network.internal.NetworkSystemImpl;
import org.terasology.persistence.StorageManager;
import org.terasology.persistence.internal.ReadWriteStorageManager;
import org.terasology.physics.CollisionGroupManager;
import org.terasology.rendering.nui.NUIManager;
import org.terasology.rendering.nui.internal.CanvasRenderer;
import org.terasology.rendering.nui.internal.NUIManagerInternal;
import org.terasology.world.biomes.BiomeManager;
import org.terasology.world.block.BlockManager;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;

/**
 * Provides a complete testing environment for modules, including assets and a world provider
 *
 * Unlike the TerasologyTestingEnvironment, assets are made available as normal within the engine. Modules are loaded automatically, component systems are created and registered, etc.
 *
 * Additionally, helpers have been provided to perform common testing manipulations, and some features have been mocked to facilitate easy testing
 *
 */

public class ModuleTestingEnvironment {
    protected static Context context;
    private static final Logger logger = LoggerFactory.getLogger(TerasologyTestingEnvironment.class);

    private static BlockManager blockManager;
    private static Config config;
    private static AudioManager audioManager;
    private static CollisionGroupManager collisionGroupManager;
    private static ModuleManager moduleManager;
    private static AssetManager assetManager;

    private static HeadlessEnvironment env;

    private EngineEntityManager engineEntityManager;
    private ComponentSystemManager componentSystemManager;
    protected EngineTime mockTime;

    @BeforeClass
    public static void setupEnvironment() throws Exception {
        final JavaArchive homeArchive = ShrinkWrap.create(JavaArchive.class);
        final FileSystem vfs = ShrinkWrapFileSystems.newFileSystem(homeArchive);
        PathManager.getInstance().useOverrideHomePath(vfs.getPath(""));
        /*
         * Create at least for each class a new headless environemnt as it is fast and prevents side effects
         * (Reusing a headless environment after other tests have modified the core registry isn't really clean)
         */
        env = new HeadlessEnvironment(new Name("engine"), new Name("ModuleTestingEnvironment"));
        context = env.getContext();
        assetManager = context.get(AssetManager.class);
        blockManager = context.get(BlockManager.class);
        config = context.get(Config.class);
        audioManager = context.get(AudioManager.class);
        collisionGroupManager = context.get(CollisionGroupManager.class);
        moduleManager = context.get(ModuleManager.class);

    }

    @Before
    public void setup() throws Exception {
        context.put(ModuleManager.class, moduleManager);

        mockTime = mock(EngineTime.class);
        context.put(Time.class, mockTime);
        NetworkSystemImpl networkSystem = new NetworkSystemImpl(mockTime, context);
        context.put(Game.class, new Game());
        context.put(NetworkSystem.class, networkSystem);
        EntitySystemSetupUtil.addReflectionBasedLibraries(context);
        EntitySystemSetupUtil.addEntityManagementRelatedClasses(context);
        engineEntityManager = context.get(EngineEntityManager.class);
        BlockManager mockBlockManager = context.get(BlockManager.class); // 'mock' added to avoid hiding a field
        BiomeManager biomeManager = context.get(BiomeManager.class);

        Path savePath = PathManager.getInstance().getSavePath("world1");
        context.put(StorageManager.class, new ReadWriteStorageManager(savePath, moduleManager.getEnvironment(),
                engineEntityManager, mockBlockManager, biomeManager));

        DisplayDevice displayDevice = new HeadlessDisplayDevice();
        context.put(DisplayDevice.class, displayDevice);
//
//        CanvasRenderer canvasRenderer = new HeadlessCanvasRenderer();
//        NUIManager nuiManager = new NUIManagerInternal(canvasRenderer, context);
//        context.put(NUIManager.class, nuiManager);

        componentSystemManager = new ComponentSystemManager(context);
        componentSystemManager.loadSystems(moduleManager.getEnvironment(), NetworkMode.DEDICATED_SERVER);
        context.put(ComponentSystemManager.class, componentSystemManager);
        LoadPrefabs prefabLoadStep = new LoadPrefabs(context);


        boolean complete = false;
        prefabLoadStep.begin();
        while (!complete) {
            complete = prefabLoadStep.step();
        }
        context.get(ComponentSystemManager.class).initialise();
        context.put(Console.class, new ConsoleImpl(context));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        env.close();
    }

    public EngineEntityManager getEntityManager() {
        return engineEntityManager;
    }
}
