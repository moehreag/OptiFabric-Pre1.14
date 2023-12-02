package me.modmuss50.optifabric.mod;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.modmuss50.optifabric.patcher.ClassCache;
import me.modmuss50.optifabric.patcher.LambadaRebuilder;
import me.modmuss50.optifabric.patcher.PatchSplitter;
import me.modmuss50.optifabric.patcher.RemapUtils;
import me.modmuss50.optifabric.util.FabricMappingConfiguration;
import me.modmuss50.optifabric.util.MappingProviderHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.mapping.reader.v2.TinyMetadata;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.TinyTree;
import net.fabricmc.tinyremapper.IMappingProvider;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

public class OptifineSetup {

	private final Path workingDir = FabricLoader.getInstance().getGameDir().resolve(".optifine");
	private Path versionDir;
	private final FabricMappingConfiguration mappingConfiguration = new FabricMappingConfiguration();

	private final FabricLauncher fabricLauncher = FabricLauncherBase.getLauncher();


	public Pair<Path, ClassCache> getRuntime() throws Throwable {
		if (!Files.exists(workingDir)) {
			Files.createDirectories(workingDir);
		}
		Path optifineModJar = OptifineVersion.findOptifineJar();

		byte[] modHash = fileHash(optifineModJar);

		versionDir = workingDir.resolve(OptifineVersion.version);
		if (!Files.exists(versionDir)) {
			Files.createDirectories(versionDir);
		}

		Path remappedJar = versionDir.resolve("Optifine-mapped.jar");
		Path optifinePatches = versionDir.resolve("Optifine.classes");

		ClassCache classCache = null;
		if (Files.exists(remappedJar) && Files.exists(optifinePatches)) {
			classCache = ClassCache.read(optifinePatches);
			//Validate that the classCache found is for the same input jar
			if (!Arrays.equals(classCache.getHash(), modHash)) {
				Optifabric.getLogger().info("Class cache is from a different optifine jar, deleting and re-generating");
				classCache = null;
				Files.delete(optifinePatches);
			}
		}

		if (Files.exists(remappedJar) && classCache != null) {
			Optifabric.getLogger().info("Found existing patched optifine jar, using that");
			return Pair.of(remappedJar, classCache);
		}

		if (OptifineVersion.jarType == OptifineVersion.JarType.OPTFINE_INSTALLER) {
			Path optifineMod = versionDir.resolve("Optifine-mod.jar");
			if (!Files.exists(optifineMod) || Files.size(optifineMod) == 0) {
				OptifineInstaller.extract(optifineModJar, optifineMod, getMinecraftJar());
			}
			optifineModJar = optifineMod;
		}

		Optifabric.getLogger().info("Setting up optifine for the first time, this may take a few seconds.");

		//A jar without srgs
		Path jarOfTheFree = versionDir.resolve("Optifine-jarofthefree.jar");
		List<String> srgs = new ArrayList<>();

		Optifabric.getLogger().info("De-Volderfiying jar");

		//Find all the SRG named classes and remove them
		{
			ZipInputStream in = new ZipInputStream(Files.newInputStream(optifineModJar));
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				String name = entry.getName();
				if (name.startsWith("com/mojang/blaze3d/platform/")) {
					if (name.contains("$")) {
						String[] split = name.replace(".class", "").split("\\$");
						if (split.length >= 2) {
							if (split[1].length() > 2) {
								srgs.add(name);
							}
						}
					}
				}

				if (name.startsWith("srg/") || name.startsWith("net/minecraft/")) {
					srgs.add(name);
				}
			}
		}

		Files.deleteIfExists(jarOfTheFree);

		{
			Files.copy(optifineModJar, jarOfTheFree);
			try (FileSystem zip = FileSystems.newFileSystem(jarOfTheFree, (ClassLoader) null)) {
				for (String s : srgs) {
					Files.deleteIfExists(zip.getPath(s));
				}
			}
		}

		Optifabric.getLogger().info("Building lambada fix mappings");
		LambadaRebuilder rebuilder = new LambadaRebuilder(jarOfTheFree, getMinecraftJar());
		rebuilder.buildLambadaMap();

		Optifabric.getLogger().info("Remapping optifine with fixed lambada names");
		Path lambadaFixJar = versionDir.resolve("Optifine-lambadafix.jar");
		RemapUtils.mapJar(lambadaFixJar, jarOfTheFree, rebuilder, getLibs());

		remapOptifine(lambadaFixJar, remappedJar);

		classCache = PatchSplitter.generateClassCache(remappedJar, optifinePatches, modHash);

		//We are done, lets get rid of the stuff we no longer need
		Files.delete(lambadaFixJar);
		Files.delete(jarOfTheFree);

		if (OptifineVersion.jarType == OptifineVersion.JarType.OPTFINE_INSTALLER) {
			Files.delete(optifineModJar);
		}

		Path extractedMappings = versionDir.resolve("mappings.tiny");
		Path fieldMappings = versionDir.resolve("mappings.full.tiny");
		Files.deleteIfExists(extractedMappings);
		Files.deleteIfExists(fieldMappings);

		boolean extractClasses = Boolean.parseBoolean(System.getProperty("optifabric.extract", "false"));
		if (extractClasses) {
			Optifabric.getLogger().info("Extracting optifine classes");
			Path optifineClasses = versionDir.resolve("optifine-classes");
			if (Files.exists(optifineClasses)) {
				Files.walkFileTree(optifineClasses, new SimpleFileVisitor<Path>(){
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
			}
			ZipInputStream in = new ZipInputStream(Files.newInputStream(remappedJar));
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				Path p = optifineClasses.resolve(entry.getName());
				if (entry.isDirectory() ) {
					Files.createDirectories(p);
				} else {
					Files.createDirectories(p.getParent());
					Files.createFile(p);
					Files.write(p, IOUtils.toByteArray(in));
				}
			}
		}

		return Pair.of(remappedJar, classCache);
	}

	private void remapOptifine(Path input, Path remappedJar) throws Exception {
		String namespace = FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
		Optifabric.getLogger().info("Remapping optifine to :" + namespace);

		List<Path> mcLibs = getLibs();
		mcLibs.add(getMinecraftJar());

		RemapUtils.mapJar(remappedJar, input, createMappings("official", namespace), mcLibs);
	}

	//Optifine currently has two fields that match the same name as Yarn mappings, we'll rename Optifine's to something else
	IMappingProvider createMappings(String from, String to) {
		//In dev
		if (fabricLauncher.isDevelopment()) {
			try {
				Path fullMappings = extractMappings();
				return (out) -> {
					RemapUtils.getTinyRemapper(fullMappings, from, to).load(out);
					//TODO use the mappings API here to stop neededing to change this each version
					out.acceptField(new IMappingProvider.Member("dbq", "CLOUDS", "Ldbe;"),
							"CLOUDS_OF");
					out.acceptField(new IMappingProvider.Member("dqr", "renderDistance", "I"),
							"renderDistance_OF");
					out.acceptField(new IMappingProvider.Member("bct", "id", "Ljava/lang/String;"),
							"id_OF");
				};
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		//In prod
		TinyTree mappingsNew = new TinyTree() {
			private final TinyTree mappings = mappingConfiguration.getMappings();

			@Override
			public TinyMetadata getMetadata() {
				return mappings.getMetadata();
			}

			@Override
			public Map<String, ClassDef> getDefaultNamespaceClassMap() {
				return mappings.getDefaultNamespaceClassMap();
			}

			@Override
			public Collection<ClassDef> getClasses() {
				return mappings.getClasses();
			}
		};
		return MappingProviderHelper.create(mappingsNew, from, to);
	}

	//Gets the minecraft libraries
	List<Path> getLibs() {
		return net.fabricmc.loader.launch.common.FabricLauncherBase.getLauncher().getLoadTimeDependencies()
				.stream().map(this::asPath).filter(Files::exists).collect(Collectors.toList());
	}

	private Path asPath(URL url) {
		try {
			return Paths.get(url.toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	//Gets the official minecraft jar
	Path getMinecraftJar() throws FileNotFoundException {
		String givenJar = System.getProperty("optifabric.mc-jar");
		if (givenJar != null) {
			Path givenJarFile = Paths.get(givenJar);

			if (Files.exists(givenJarFile)) {
				return givenJarFile;
			} else {
				System.err.println("Supplied Minecraft jar at " + givenJar + " doesn't exist, falling back");
			}
		}

		Path minecraftJar = getLaunchMinecraftJar();

		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {

			Path officialNames = minecraftJar.resolveSibling(String.format("minecraft-%s-client.jar", OptifineVersion.minecraftVersion));

			if (Files.notExists(officialNames)) {
				Path parent = minecraftJar.getParent().resolveSibling(String.format("minecraft-%s-client.jar", OptifineVersion.minecraftVersion));

				if (Files.notExists(parent)) {
					Path alternativeParent = parent.resolveSibling("minecraft-client.jar");

					if (Files.notExists(alternativeParent)) {
						try {
							return getGameJar();
						} catch (IOException e) {
							throw new AssertionError("Unable to find Minecraft dev jar! Tried " + officialNames + ", " + parent + " and " + alternativeParent
									+ "\nPlease supply it explicitly with -Doptifabric.mc-jar");
						}
					}

					parent = alternativeParent;
				}

				officialNames = parent;
			}

			minecraftJar = officialNames;
		}

		return minecraftJar;
	}

	private Path getGameJar() throws IOException {
		Path gameJar = versionDir.resolve("game.jar");
		if (Files.exists(gameJar) && Files.size(gameJar) != 0) {
			return gameJar;
		}
		try (InputStream connection = URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").toURL().openStream()) {
			JsonObject object = new GsonBuilder().create().fromJson(new InputStreamReader(connection), JsonObject.class);
			object.get("versions").getAsJsonArray().forEach(e -> {
				if (e.getAsJsonObject().get("id").getAsString().equals("1.8.9")) {
					String url = e.getAsJsonObject().get("url").getAsString();

					try (InputStream stream = URI.create(url).toURL().openStream()) {
						JsonObject downloads = new GsonBuilder().create().fromJson(new InputStreamReader(stream), JsonObject.class).get("downloads").getAsJsonObject();
						String clientUrl = downloads.get("client").getAsJsonObject().get("url").getAsString();

						Files.copy(URI.create(clientUrl).toURL().openStream(), gameJar);
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				}
			});
		}
		return gameJar;
	}

	private static Path getLaunchMinecraftJar() {
		try {
			return (Path) FabricLoader.getInstance().getObjectShare().get("fabric-loader:inputGameJar");
		} catch (NoClassDefFoundError | NoSuchMethodError old) {
			ModContainer mod = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow(() -> new IllegalStateException("No Minecraft?"));
			URI uri = mod.getRootPaths().get(0).toUri();
			assert "jar".equals(uri.getScheme());

			String path = uri.getSchemeSpecificPart();
			int split = path.lastIndexOf("!/");

			if (path.substring(0, split).indexOf(' ') > 0 && path.startsWith("file:///")) {//This is meant to be a URI...
				Path out = Paths.get(path.substring(8, split));
				if (Files.exists(out)) return out;
			}

			try {
				return Paths.get(new URI(path.substring(0, split)));
			} catch (URISyntaxException e) {
				throw new RuntimeException("Failed to find Minecraft jar from " + uri + " (calculated " + path.substring(0, split) + ')', e);
			}
		}
	}

	//Extracts the devtime mappings out of yarn into a file
	Path extractMappings() throws IOException {
		Path extractedMappings = versionDir.resolve("mappings.tiny");
		if (Files.exists(extractedMappings)) {
			Files.delete(extractedMappings);
		}
		InputStream mappingStream = FabricLauncherBase.class.getClassLoader().getResourceAsStream("mappings/mappings.tiny");
		if (mappingStream != null) {
			Files.copy(mappingStream, extractedMappings);
			if (!Files.exists(extractedMappings)) {
				throw new RuntimeException("failed to extract mappings!");
			}
			return extractedMappings;
		} else {
			return null;
		}
	}

	byte[] fileHash(Path input) throws IOException {
		try (InputStream is = Files.newInputStream(input)) {
			return DigestUtils.md5(is);
		}
	}
}
