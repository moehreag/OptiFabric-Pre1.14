package me.modmuss50.optifabric.util;

import java.io.File;
import java.util.function.Supplier;

import net.minecraft.client.resource.pack.ResourcePack;
import net.minecraft.client.resource.pack.ZippedResourcePack;

public class OptifineZipResourcePack extends ZippedResourcePack {
	public OptifineZipResourcePack(File file) {
		super(file);
	}

	@Override
	public String getName() {
		return "Optifine Internal Resources";
	}

	public static Supplier<ResourcePack> getSupplier(File file) {
		return () -> new OptifineZipResourcePack(file);
	}
}
