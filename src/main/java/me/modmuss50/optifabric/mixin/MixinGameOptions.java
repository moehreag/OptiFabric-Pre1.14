package me.modmuss50.optifabric.mixin;

import me.modmuss50.optifabric.mod.Optifabric;
import net.minecraft.client.options.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Mixin(GameOptions.class)
public class MixinGameOptions {

	@Shadow public List<String> resourcePacks;

	@Shadow private File file;

	@SuppressWarnings({"ResultOfMethodCallIgnored", "UnresolvedMixinReference"})
	@Inject(method = "m_2009088", at = @At("RETURN"), remap = false) //m_2009088 = load
	private void load(CallbackInfo info) {
		File optifabricOptions = new File(file.getParent(), "optifabric.txt");
		if (!optifabricOptions.exists()) {

			//Add optifine to resource packs if optifabric.txt doesnt exist, makes it default on, but can be disabled.
			if (!resourcePacks.contains("optifine")) {
				resourcePacks.add("optifine");
			}

			try {
				optifabricOptions.createNewFile();
			} catch (IOException e) {
				Optifabric.getLogger().get().error("Error while creating options file:", e);
			}
		}
	}
}
