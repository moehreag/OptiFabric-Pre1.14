package me.modmuss50.optifabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Suppresses some warnings in the log
@Pseudo
@Mixin(targets = "net.optifine.reflect.ReflectorClass")
public class MixinReflectorClass {

	@Shadow(remap = false)
	private String targetClassName;

	@Shadow(remap = false)
	private boolean checked;

	@SuppressWarnings("UnresolvedMixinReference")
	@Inject(method = "getTargetClass", at = @At("HEAD"), remap = false)
	private void getTargetClass(CallbackInfoReturnable<Class<?>> infoReturnable) {
		if (!checked) {//Only check the target if it hasn't been done yet
			String name = targetClassName.replaceAll("/", ".");
			if (name.startsWith("net.minecraft.launchwrapper") || name.startsWith("net.minecraftforge") || "optifine.OptiFineClassTransformer".equals(name)) {
				checked = true;
			}
		}
	}

}
