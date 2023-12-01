package me.modmuss50.optifabric.mod;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import me.modmuss50.optifabric.patcher.ASMUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class OptifineVersion {


	public static String version;
	public static String minecraftVersion;
	public static JarType jarType;

	public static Path findOptifineJar() throws IOException {
		Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
		Path[] mods = Files.list(modsDir).toArray(Path[]::new);

		Path optifineJar = null;

		for (Path file : mods) {
			if (Files.isDirectory(file)) {
				continue;
			}
			if (file.getFileName().toString().endsWith(".jar")) {
				JarType type = getJarType(file);
				if (type.error) {
					if (!type.name().equals("INCOMPATIBE")) {
						throw new RuntimeException("An error occurred when trying to find the optifine jar: " + type.name());
					} else {
						continue;
					}
				}
				if (type == JarType.OPIFINE_MOD || type == JarType.OPTFINE_INSTALLER) {
					if (optifineJar != null) {
						OptifabricError.setError("Found 2 or more optifine jars, please ensure you only have 1 copy of optifine in the mods folder!");
						throw new FileNotFoundException("Multiple optifine jars");
					}
					jarType = type;
					optifineJar = file;
				}
			}
		}

		if (optifineJar != null) {
			return optifineJar;
		}

		OptifabricError.setError("OptiFabric could not find the Optifine jar in the mods folder.");
		throw new FileNotFoundException("Could not find optifine jar");
	}

	private static JarType getJarType(Path file) throws IOException {
		ClassNode classNode = null;
		try (JarInputStream in = new JarInputStream(Files.newInputStream(file))) {
			JarEntry jarEntry;
			while ((jarEntry = in.getNextJarEntry()) != null) {
				if (jarEntry.getName().equals("Config.class")) {
					classNode = ASMUtils.readClassFromBytes(IOUtils.toByteArray(in));
					break;
				}
			}
			if (classNode == null) {
				return JarType.SOMETHINGELSE;
			}
		}

		for (FieldNode fieldNode : classNode.fields) {
			if (fieldNode.name.equals("VERSION")) {
				version = (String) fieldNode.value;
			}
			if (fieldNode.name.equals("MC_VERSION")) {
				minecraftVersion = (String) fieldNode.value;
			}
		}

		if (version == null || version.isEmpty() || minecraftVersion == null || minecraftVersion.isEmpty()) {
			return JarType.INCOMPATIBE;
		}

		String currentMcVersion = "1.8.9";

		if (!currentMcVersion.equals(minecraftVersion)) {
			OptifabricError.setError(String.format("This version of optifine is not compatible with the current minecraft version\n\n Optifine requires %s you have %s", minecraftVersion, currentMcVersion));
			return JarType.INCOMPATIBE;
		}

		Holder<Boolean> isInstaller = new Holder<>(false);
		ZipInputStream input = new ZipInputStream(Files.newInputStream(file));
		ZipEntry zipEntry;
		while ((zipEntry = input.getNextEntry()) != null) {
			if (zipEntry.getName().startsWith("patch/")) {
				isInstaller.setValue(true);
			}
		}

		if (isInstaller.getValue()) {
			return JarType.OPTFINE_INSTALLER;
		} else {
			return JarType.OPIFINE_MOD;
		}
	}

	public enum JarType {
		OPIFINE_MOD(false),
		OPTFINE_INSTALLER(false),
		INCOMPATIBE(true),
		SOMETHINGELSE(false);

		boolean error;

		JarType(boolean error) {
			this.error = error;
		}

		public boolean isError() {
			return error;
		}
	}

	private static class Holder<T> {

		T value;

		private Holder(T value) {
			this.value = value;
		}

		public T getValue() {
			return value;
		}

		public void setValue(T value) {
			this.value = value;
		}

		public static <T> Holder<T> of(T value) {
			return new Holder<>(value);
		}

	}

}
