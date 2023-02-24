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
import static pl.skidam.yetanotherauthmod.yaam.onlineUUIDs;

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
            String playerName = packet.name();
            String playerUUID = packet.profileId().isPresent() ? packet.profileId().get().toString().replace("-", "").toLowerCase() : null;

            if (playerUUID == null) {
                LOGGER.info("Authenticating " + playerName + " as non-premium player.");
                this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                this.profile = new GameProfile(null, playerName);
                ci.cancel();

            } else if (onlineUUIDs.contains(playerUUID)) {
                LOGGER.info("Authenticating " + playerName + " as premium player.");
                // original mojang auth... (look at the source of the mixin)

            } else {
                String purchasedUUID = Utils.hasPurchasedMinecraft(playerName);

                if (purchasedUUID == null) {
                    LOGGER.info("Authenticating " + playerName + " as non-premium player.");
                    this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    this.profile = new GameProfile(null, playerName);
                    ci.cancel();

                } else if (purchasedUUID.equals(playerUUID)) {
                    LOGGER.info("Authenticating " + playerName + " as premium player.");
                    onlineUUIDs.add(purchasedUUID);
                    // original mojang auth... (look at the source of the mixin)

                } else { // player using premium username without access to this premium account

                    // TODO add all premium players to login database, and if this player is not in database, let this offline player join

                    Text reason = Text.literal("This username is taken!\n").formatted(Formatting.RED).append("Please buy original copy of the game or change your username to play!").formatted(Formatting.RED);
                    connection.send(new LoginDisconnectS2CPacket(reason));
                    connection.disconnect(reason);
                    ci.cancel();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

            // kick player on error
            Text reason = Text.literal("Authentication error. Please contact server admin!\n").formatted(Formatting.RED).append("[" + e.getMessage() + "] More details in server log.").formatted(Formatting.RED);
            connection.send(new LoginDisconnectS2CPacket(reason));
            connection.disconnect(reason);
            ci.cancel();
        }
    }
}
