package me.modmuss50.optifabric.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureAtlas.class)
public class MixinTextureAtlas {

	@Redirect(method = "bindAndTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;bind(I)V"))
	private void bind(int i){
		GlStateManager.bindTexture(i);
	}
}
