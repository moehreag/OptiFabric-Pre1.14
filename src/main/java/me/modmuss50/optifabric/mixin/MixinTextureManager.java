package me.modmuss50.optifabric.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureManager.class)
public class MixinTextureManager {

	@Redirect(method = "bind", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;bind(I)V"))
	private void bind(int i){
		GlStateManager.bindTexture(i);
	}
}
