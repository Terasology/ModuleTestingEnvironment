// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.moduletestingenvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.TerasologyEngine;
import org.terasology.engine.module.ClasspathSupportingModuleLoader;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.paths.PathManager;
import org.terasology.module.Module;
import org.terasology.module.ModuleLoader;
import org.terasology.module.ModuleMetadataJsonAdapter;
import org.terasology.module.ModuleRegistry;
import org.terasology.utilities.Jvm;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleLoadingStuff {
    private static final Logger logger = LoggerFactory.getLogger(ModuleLoadingStuff.class);

    private static ModuleRegistry registry;
    private static ModuleLoader classpathLoader;
    private static PathManager pathManager;

    public ModuleLoadingStuff(TerasologyEngine engine) {
        ModuleManager manager = engine.getFromEngineContext(ModuleManager.class);
        pathManager = PathManager.getInstance();
        registry = manager.getRegistry();
        ModuleMetadataJsonAdapter metadataReader = manager.getModuleMetadataReader();
        classpathLoader = new ClasspathSupportingModuleLoader(metadataReader, true, true);
        classpathLoader.setModuleInfoPath(TerasologyConstants.MODULE_INFO_FILENAME);

    }

    void loadModulesFromClasspath() {
        logger.debug("loadModulesFromClassPath with classpath:");
        Jvm.logClasspath(logger);

        Stream<Path> allClassPaths = Arrays.stream(
                System.getProperty("java.class.path").split(System.getProperty("path.separator", ":"))
        ).map(Paths::get).filter(path -> path.toFile().exists());
        Stream<Path> pathsOutsideModulesDirectory = allClassPaths.filter(classPath -> pathManager.getModulePaths().stream().anyMatch(
                other -> !classPath.startsWith(other)
        ));
        List<Path> classPaths = pathsOutsideModulesDirectory.collect(Collectors.toList());

        // I thought I'd make the ClasspathSupporting stuff in the shape of a ModuleLoader
        // so I could use it with the existing ModulePathScanner, but no. The inputs to that
        // are the _parent directories_ of what we have.
        for (Path path : classPaths) {
            attemptToLoadModule(path);
        }
    }

    public void attemptToLoadModule(Path path) {
        // The conditions here mirror those of org.terasology.module.ModulePathScanner.loadModule

        Module module;
        try {
            module = classpathLoader.load(path);
        } catch (IOException e) {
            logger.error("Failed to load classpath module {}", path, e);
            return;
        }

        if (module == null) {
            return;
        }

        boolean isNew = registry.add(module);
        if (isNew) {
            logger.info("Discovered module: {} on classpath as {}", module, path.getFileName());
        } else {
            logger.warn("Discovered duplicate module: {}-{}, skipping {}",
                    module.getId(), module.getVersion(), path);
        }
    }
}
