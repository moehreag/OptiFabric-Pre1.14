package me.modmuss50.optifabric.patcher;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import me.modmuss50.optifabric.mod.Optifabric;
import org.apache.commons.io.IOUtils;

//Pulls out the patched classes and saves into a classCache, and also creates an optifine jar without these classes
public class PatchSplitter {

	public static ClassCache generateClassCache(Path inputFile, Path classCacheOutput, byte[] inputHash) throws IOException {
		boolean extractClasses = Boolean.parseBoolean(System.getProperty("optifabric.extract", "false"));
		Path classesDir = classCacheOutput.getParent().resolve("classes");
		if (extractClasses) {
			Files.createDirectory(classesDir);
		}
		ClassCache classCache = new ClassCache(inputHash);
		try (JarInputStream jarFile = new JarInputStream(Files.newInputStream(inputFile))) {
			JarEntry entry;
			while ((entry = jarFile.getNextJarEntry()) != null) {
				if ((entry.getName().startsWith("net/minecraft/") || entry.getName().startsWith("com/mojang/")) && entry.getName().endsWith(".class")) {

					String name = entry.getName();
					byte[] bytes = IOUtils.toByteArray(jarFile);
					classCache.addClass(name, bytes);
					if (extractClasses) {
						Path classFile = classesDir.resolve(entry.getName());
						if (!Files.exists(classFile.getParent())){
							Files.createDirectories(classFile.getParent());
						}
						Files.write(classFile, bytes);
					}
				}
			}
		}


		//Remove all the classes that are going to be patched in, we don't want theses on the classpath
		try (FileSystem zip = FileSystems.newFileSystem(inputFile, (ClassLoader) null)) {
			for (String s : classCache.getClasses()) {
				Files.deleteIfExists(zip.getPath(s));
			}
		}

		Optifabric.getLogger().info("Found " + classCache.getClasses().size() + " patched classes");
		classCache.save(classCacheOutput);
		return classCache;
	}

}
