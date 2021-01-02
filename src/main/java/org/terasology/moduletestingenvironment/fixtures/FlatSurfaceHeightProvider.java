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
package org.terasology.moduletestingenvironment.fixtures;

import org.joml.Vector2ic;
import org.terasology.world.generation.FacetProvider;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Produces;
import org.terasology.world.generation.facets.ElevationFacet;
import org.terasology.world.generation.facets.SurfacesFacet;

@Produces({SurfacesFacet.class, ElevationFacet.class})
public class FlatSurfaceHeightProvider implements FacetProvider {
    private int height;

    public FlatSurfaceHeightProvider(int height) {
        this.height = height;
    }

    @Override
    public void setSeed(long seed) {
    }

    @Override
    public void process(GeneratingRegion region) {
        ElevationFacet elevationFacet = new ElevationFacet(region.getRegion(), region.getBorderForFacet(ElevationFacet.class));
        SurfacesFacet surfacesFacet = new SurfacesFacet(region.getRegion(), region.getBorderForFacet(SurfacesFacet.class));

        for (Vector2ic pos : elevationFacet.getRelativeRegion()) {
            elevationFacet.set(pos, height);
        }

        if (surfacesFacet.getWorldRegion().minY() <= height && height <= surfacesFacet.getWorldRegion().maxY()) {
            for (int x = surfacesFacet.getWorldRegion().minX(); x <= surfacesFacet.getWorldRegion().maxX(); x++) {
                for (int z = surfacesFacet.getWorldRegion().minZ(); z <= surfacesFacet.getWorldRegion().maxZ(); z++) {
                    surfacesFacet.setWorld(x, height, z, true);
                }
            }
        }

        region.setRegionFacet(ElevationFacet.class, elevationFacet);
        region.setRegionFacet(SurfacesFacet.class, surfacesFacet);
    }
}

