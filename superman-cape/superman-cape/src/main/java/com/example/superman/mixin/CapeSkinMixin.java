package com.example.superman.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the cape texture of the local player with our red cape.
 *
 * NOTE: this is the part most sensitive to Minecraft version changes
 * (PlayerSkin / ClientAsset / Identifier were reworked in 1.21.9-1.21.11).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class CapeSkinMixin {
    private static final ClientAsset.Texture SUPERMAN_CAPE =
            new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("supermancape", "textures/cape.png"));

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void supermancape$replaceCape(CallbackInfoReturnable<PlayerSkin> cir) {
        if ((Object) this != Minecraft.getInstance().player) return;
        PlayerSkin skin = cir.getReturnValue();
        cir.setReturnValue(new PlayerSkin(
                skin.body(),
                SUPERMAN_CAPE,
                skin.elytra(),
                skin.model(),
                skin.secure()
        ));
    }
}
