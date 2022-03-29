package me.modmuss50.optifabric.mixin;

import me.modmuss50.optifabric.mod.Optifabric;
import me.modmuss50.optifabric.mod.OptifabricError;
import me.modmuss50.optifabric.mod.OptifineVersion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

	protected MixinTitleScreen() {
		super();
	}

	@Inject(method = "method_21947", at = @At("RETURN"))
	private void init(CallbackInfo info) {
		Optifabric.checkForErrors();
	}

	@Inject(method = "method_21925", at = @At("RETURN"))
	private void render(int int_1, int int_2, float float_1, CallbackInfo info) {
		if (!OptifabricError.hasError()) {
			this.method_21884(MinecraftClient.getInstance().textRenderer, OptifineVersion.version, 2, this.field_22536 - 20, 0xFFFFFFFF);
		}
	}
}
