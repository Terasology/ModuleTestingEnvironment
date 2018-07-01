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
package org.terasology.rendering.opengl;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * Dummy version of the class by the same name provided by the Terasology engine to avoid certain usages making no sense for the MTE.
 */
public class ScreenGrabber {

    public ScreenGrabber() {

    }

    public float getExposure() {
        return 0.0f;
    }

    // TODO: Remove this method, temporarily here for DownSampleSceneAndUpdateExposure
    public void setExposure(float exposure) {

    }

    public void takeScreenshot() {

    }


    public void saveScreenshot() {

    }

    private void saveScreenshotTask(ByteBuffer buffer, int width, int height) {

    }

    private void saveGamePreviewTask(ByteBuffer buffer, int width, int height) {

    }

    private void writeImageToFile(BufferedImage image, Path path, String format) {

    }

    private BufferedImage convertByteBufferToBufferedImage(ByteBuffer buffer, int width, int height) {
        return null;
    }

    private Path getScreenshotPath(final int width, final int height, final String format) {
        return null;
    }


    public boolean isTakingScreenshot() {
        return false;
    }


    public void takeGamePreview(Path path) {

    }
}
