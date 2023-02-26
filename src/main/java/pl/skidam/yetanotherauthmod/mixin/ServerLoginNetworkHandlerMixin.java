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
import pl.skidam.yetanotherauthmod.TextHelper;
import pl.skidam.yetanotherauthmod.Utils;
import pl.skidam.yetanotherauthmod.yaam;

import static pl.skidam.yetanotherauthmod.yaam.LOGGER;

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

            if (playerUUID == null) { // it should never happen
                LOGGER.error("{} UUID is null!", playerName);
                Text reason = TextHelper.literal("Authentication error. UUID is null?").formatted(Formatting.RED);
                connection.send(new LoginDisconnectS2CPacket(reason));
                connection.disconnect(reason);
                ci.cancel();

            } else if (yaam.database.checkLogin(playerUUID, null)) { // 100% premium player
                LOGGER.info("Authenticating {} as premium player.", playerName);
                // original mojang auth... (look at the source of the mixin)

            } else {
                String purchasedUUID = Utils.hasPurchasedMinecraft(playerName.toLowerCase());

                if (purchasedUUID == null) { // 100% non-premium player
                    LOGGER.info("Authenticating {} as non-premium player.", playerName);
                    this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    this.profile = new GameProfile(null, playerName);
                    ci.cancel();

                } else if (purchasedUUID.equals(playerUUID)) { // 100% premium player
                    LOGGER.info("Authenticating {} as premium player.", playerName);
                    yaam.database.addUser(purchasedUUID, null);
                    // original mojang auth... (look at the source of the mixin)

                } else if (yaam.database.userExists(purchasedUUID)) { // 100% non-premium: player trying to use premium username without access to this premium account
                    Text reason = TextHelper.literal("This username is taken!\n").formatted(Formatting.RED).append("Please buy original copy of the game or change your username to play!").formatted(Formatting.RED);
                    connection.send(new LoginDisconnectS2CPacket(reason));
                    connection.disconnect(reason);
                    ci.cancel();

                } else { // 100% non-premium: player using premium username but the original owner of this username is not playing on this server
                    LOGGER.info("Authenticating {} as non-premium player.", playerName);
                    this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    this.profile = new GameProfile(null, playerName);
                    ci.cancel();
                }
            }
        } catch (Exception e) { // Probably mojang api down
            e.printStackTrace();

            // kick player on error
            Text reason = TextHelper.literal("Authentication error. Please contact server admin!\n").formatted(Formatting.RED).append(e.getMessage() + "\n").formatted(Formatting.DARK_RED).append("More details in server log.").formatted(Formatting.RED);
            connection.send(new LoginDisconnectS2CPacket(reason));
            connection.disconnect(reason);
            ci.cancel();
        }
    }
}
