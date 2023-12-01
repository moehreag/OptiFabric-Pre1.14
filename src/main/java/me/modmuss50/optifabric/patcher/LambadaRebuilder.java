package me.modmuss50.optifabric.patcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.MemberInstance;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class LambadaRebuilder implements IMappingProvider {

	private Path optifineFile;
	private Path minecraftClientFile;

	private JarInputStream optifineJar;
	private JarInputStream clientJar;

	private Map<String, String> methodMap = new HashMap<>();
	private List<String> usedMethods = new ArrayList<>(); //Used to prevent duplicates

	public LambadaRebuilder(Path optifineFile, Path minecraftClientFile) throws IOException {
		this.optifineFile = optifineFile;
		this.minecraftClientFile = minecraftClientFile;
		optifineJar = new JarInputStream(Files.newInputStream(optifineFile));
		clientJar = new JarInputStream(Files.newInputStream(minecraftClientFile));

	}

	public void buildLambadaMap() throws IOException {
		JarEntry entry;
		while ((entry = optifineJar.getNextJarEntry()) != null) {
			if (entry.getName().endsWith(".class") && !entry.getName().startsWith("net/") && !entry.getName().startsWith("optifine/") && !entry.getName().startsWith("javax/")) {
				buildClassMap(entry);
			}
		}
		optifineJar.close();
		clientJar.close();
	}

	private void buildClassMap(JarEntry jarEntry) throws IOException {
		ClassNode classNode = ASMUtils.readClassFromBytes(IOUtils.toByteArray(optifineJar));
		List<MethodNode> lambadaNodes = new ArrayList<>();
		for (MethodNode methodNode : classNode.methods) {
			if (!methodNode.name.startsWith("lambda$") || methodNode.name.startsWith("lambda$static")) {
				continue;
			}
			lambadaNodes.add(methodNode);
		}
		if (lambadaNodes.isEmpty()) {
			return;
		}
		ClassNode minecraftClass = ASMUtils.readClassFromBytes(IOUtils.toByteArray(clientJar));
		if (!minecraftClass.name.equals(classNode.name)) {
			throw new RuntimeException("Something went wrong");
		}
		for (MethodNode methodNode : lambadaNodes) {
			MethodNode actualNode = findMethod(methodNode, classNode, minecraftClass);
			if (actualNode == null) {
				continue;
			}
			String key = classNode.name + "." + MemberInstance.getMethodId(actualNode.name, actualNode.desc);
			if (usedMethods.contains(key)) {
				System.out.println("Skipping duplicate: " + key);
				continue;
			}
			usedMethods.add(classNode.name + "." + MemberInstance.getMethodId(actualNode.name, actualNode.desc));
			methodMap.put(classNode.name + "/" + MemberInstance.getMethodId(methodNode.name, methodNode.desc), actualNode.name);
		}
	}

	private MethodNode findMethod(MethodNode optifineMethod, ClassNode optifineClass, ClassNode minecraftClass) {
		{
			MethodNode lastNode = null;
			int identiacalMethods = 0;
			for (MethodNode methodNode : minecraftClass.methods) {
				if (ASMUtils.isSynthetic(methodNode.access) && methodNode.desc.equals(optifineMethod.desc)) {
					identiacalMethods++;
					lastNode = methodNode;
				}
			}
			if (identiacalMethods == 1) {
				return lastNode;
			}
		}

		//TODO some room for some better detection here

		return null;
	}

	@Override
	public void load(MappingAcceptor out) {
		methodMap.putAll(this.methodMap);
	}
}
