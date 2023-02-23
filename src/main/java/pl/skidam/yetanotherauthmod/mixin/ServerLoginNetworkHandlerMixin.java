package pl.skidam.yetanotherauthmod.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.skidam.yetanotherauthmod.Utils;

import java.io.IOException;

import static pl.skidam.yetanotherauthmod.yaam.LOGGER;
import static pl.skidam.yetanotherauthmod.yaam.mojangAccounts;

@Mixin(value = ServerLoginNetworkHandler.class, priority = 2137)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    GameProfile profile;
    @Shadow
    ServerLoginNetworkHandler.State state;
    @Shadow @Mutable @Final
    public ClientConnection connection;

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
    private void onHello(LoginHelloC2SPacket packet, CallbackInfo ci) {
        try {

            // It needs to be lowercase otherwise mojang api not work as expected
            String playerName = packet.name();

            if (!mojangAccounts.contains(playerName.toLowerCase())) {
                if (Utils.hasPurchasedMinecraft(playerName.toLowerCase())) {
                    LOGGER.info(playerName + " is premium player.");
                    mojangAccounts.add(playerName.toLowerCase());
                } else {
                    LOGGER.info(playerName + " is non premium player.");
                    this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    this.profile = new GameProfile(null, packet.name());
                    ci.cancel();
                }
            } else {
                LOGGER.info(playerName + " is premium player.");
            }

        } catch (IOException e) {
            e.printStackTrace();

            // kick player on error
            Text reason = Text.literal("Authentication error. Please contact server admin!\n").formatted(Formatting.RED).append("[" + e.getMessage() + "] More details in server log.").formatted(Formatting.RED);
            connection.send(new LoginDisconnectS2CPacket(reason));
            connection.disconnect(reason);
        }
    }
}
