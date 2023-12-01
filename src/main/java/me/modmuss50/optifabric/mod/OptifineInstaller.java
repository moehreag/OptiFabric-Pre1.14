package me.modmuss50.optifabric.mod;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

//A class used to extract the optifine jar from the installer
public class OptifineInstaller {

	public static void extract(Path installer, Path output, Path minecraftJar) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, MalformedURLException {
		Optifabric.getLogger().info("Running optifine patcher");
		ClassLoader classLoader = new URLClassLoader(new URL[]{installer.toUri().toURL()}, OptifineInstaller.class.getClassLoader());
		Class<?> clazz = classLoader.loadClass("optifine.Patcher");
		Method method = clazz.getDeclaredMethod("process", File.class, File.class, File.class);
		method.invoke(null, minecraftJar.toFile(), installer.toFile(), output.toFile());
	}

}
