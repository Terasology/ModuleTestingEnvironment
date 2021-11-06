// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

/**
 * Provides a Terasology engine for use with unit tests.
 * <p>
 * Key points of interest for test authors are:
 * <ul>
 *     <li>{@link org.terasology.moduletestingenvironment.MTEExtension MTEExtension}: Use this on your JUnit 5 test classes.
 *     <li>{@link org.terasology.moduletestingenvironment.MainLoop MainLoop}: Methods for running the engine during your test scenarios.
 *     <li>{@link org.terasology.moduletestingenvironment.Engines}: You can add additional engines to simulate remote connections to the
 *         host. <i>[Experimental]</i>
 * </ul>
 */
package org.terasology.moduletestingenvironment;

