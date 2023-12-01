/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.modmuss50.optifabric.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.zip.ZipError;

import me.modmuss50.optifabric.mod.Optifabric;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

public final class FabricMappingConfiguration {
	private boolean initialized;
	private String gameId;
	private String gameVersion;
	private TinyTree mappings;

	public FabricMappingConfiguration() {
	}

	public String getGameId() {
		this.initialize();
		return this.gameId;
	}

	public String getGameVersion() {
		this.initialize();
		return this.gameVersion;
	}

	public boolean matches(String gameId, String gameVersion) {
		this.initialize();
		return (this.gameId == null || gameId == null || gameId.equals(this.gameId))
				&& (this.gameVersion == null || gameVersion == null || gameVersion.equals(this.gameVersion));
	}

	public TinyTree getMappings() {
		this.initialize();
		return this.mappings;
	}

	public String getTargetNamespace() {
		return FabricLauncherBase.getLauncher().isDevelopment() ? "named" : "intermediary";
	}

	public boolean requiresPackageAccessHack() {
		return this.getTargetNamespace().equals("named");
	}

	private void initialize() {
		if (!this.initialized) {
			URL url = FabricMappingConfiguration.class.getClassLoader().getResource("mappings/mappings.tiny");
			if (url != null) {
				try {
					URLConnection connection = url.openConnection();
					if (connection instanceof JarURLConnection) {
						Manifest manifest = ((JarURLConnection) connection).getManifest();
						if (manifest != null) {
							this.gameId = manifest.getMainAttributes().getValue(new Name("Game-Id"));
							this.gameVersion = manifest.getMainAttributes().getValue(new Name("Game-Version"));
						}
					}

					BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

					try {
						long time = System.currentTimeMillis();
						this.mappings = TinyMappingFactory.loadWithDetection(reader);
						Optifabric.getLogger().debug("Mappings", "Loading mappings took %d ms", System.currentTimeMillis() - time);
					} catch (Throwable var7) {
						try {
							reader.close();
						} catch (Throwable var6) {
							var7.addSuppressed(var6);
						}

						throw var7;
					}

					reader.close();
				} catch (ZipError | IOException var8) {
					throw new RuntimeException("Error reading " + url, var8);
				}
			}

			if (this.mappings == null) {
				Optifabric.getLogger().info("Mappings", "Mappings not present!");
				this.mappings = TinyMappingFactory.EMPTY_TREE;
			}

			this.initialized = true;
		}
	}
}
