package pl.skidam.yetanotherauthmod.mixin;

import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {

    @Inject(method = "onHello", at = @At("HEAD"), cancellable = true)
    public void onHello(LoginHelloC2SPacket packet, CallbackInfo ci) {
        System.out.println("Hello from mixin! " + packet.name() + " " + packet.profileId());
    }

    @Inject(
            method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V",
            at = @At(
                    value = "NEW",
                    target = "com/mojang/authlib/GameProfile",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            cancellable = true
    )
    private void checkPremium(LoginHelloC2SPacket packet, CallbackInfo ci) {
        System.out.println("Hello from mixin! " + packet.name() + " " + packet.profileId());
    }
}
