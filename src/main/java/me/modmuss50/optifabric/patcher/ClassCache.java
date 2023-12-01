package me.modmuss50.optifabric.patcher;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.Validate;

public class ClassCache {

	private byte[] hash;
	private Map<String, byte[]> classes = new HashMap<>();

	public ClassCache(byte[] hash) {
		this.hash = hash;
	}

	private ClassCache() {
	}

	public void addClass(String name, byte[] bytes) {
		if (classes.containsKey(name)) {
			throw new UnsupportedOperationException(name + " is already in ClassCache");
		}
		Validate.notNull(bytes, "bytes cannot be null");
		classes.put(name, bytes);
	}

	public byte[] getClass(String name) {
		return classes.get(name);
	}

	public byte[] getAndRemove(String name) {
		byte[] bytes = getClass(name);
		classes.remove(name);
		return bytes;
	}

	public byte[] getHash() {
		return hash;
	}

	public Set<String> getClasses() {
		return classes.keySet();
	}

	public static ClassCache read(Path input) throws IOException {
		InputStream in = Files.newInputStream(input);
		GZIPInputStream gis = new GZIPInputStream(in);
		DataInputStream dis = new DataInputStream(gis);

		ClassCache classCache = new ClassCache();

		//Read the hash
		int hashSize = dis.readInt();
		byte[] hash = new byte[hashSize];
		dis.readFully(hash);
		classCache.hash = hash;

		int count = dis.readInt();
		for (int i = 0; i < count; i++) {
			int nameByteCount = dis.readInt();
			byte[] nameBytes = new byte[nameByteCount];
			dis.readFully(nameBytes);
			String name = new String(nameBytes, StandardCharsets.UTF_8);

			int byteCount = dis.readInt();
			byte[] bytes = new byte[byteCount];
			dis.readFully(bytes);
			classCache.classes.put(name, bytes);
		}

		dis.close();
		gis.close();
		in.close();
		return classCache;
	}

	public void save(Path output) throws IOException {
		if (Files.exists(output)) {
			Files.delete(output);
		}
		OutputStream out = Files.newOutputStream(output);
		GZIPOutputStream gos = new GZIPOutputStream(out);
		DataOutputStream dos = new DataOutputStream(gos);

		//Write the hash
		dos.writeInt(hash.length);
		dos.write(hash);

		//Write the number of classes
		dos.writeInt(classes.size());
		for (Map.Entry<String, byte[]> clazz : classes.entrySet()) {
			String name = clazz.getKey();
			byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
			byte[] bytes = clazz.getValue();

			//Write the name
			dos.writeInt(nameBytes.length);
			dos.write(nameBytes);

			//Write the actual bytes
			dos.writeInt(bytes.length);
			dos.write(bytes);
		}
		dos.flush();
		dos.close();
		gos.flush();
		gos.close();
		out.flush();
		out.close();
	}

}
