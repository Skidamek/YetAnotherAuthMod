package pl.skidam.yetanotherauthmod.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static pl.skidam.yetanotherauthmod.yaam.mojangAccountNamesCache;

@Mixin(value = ServerLoginNetworkHandler.class, priority = 2137)
public abstract class ServerLoginNetworkHandlerMixin {

    @Shadow
    GameProfile profile;

    @Shadow
    ServerLoginNetworkHandler.State state;

    /**
     * Checks whether the player has purchased an account.
     * If so, server is presented as online, and continues as in normal-online mode.
     * Otherwise, player is marked as ready to be accepted into the game.
     *
     * @param packet
     * @param ci
     */
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
        System.out.println("Hello packet received");
        try {
            System.out.println("Checking player " + packet.name() + " account status");
            String playerName = (new GameProfile(null, packet.name())).getName().toLowerCase();
            // Checking account status from API
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName).openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setConnectTimeout(5000);
            httpsURLConnection.setReadTimeout(5000);


            int response = httpsURLConnection.getResponseCode();
            httpsURLConnection.disconnect();

            System.out.println(response);

            if (response == HttpURLConnection.HTTP_OK) { // Player has a Mojang account
                System.out.println("Player " + playerName + " has a Mojang account");
                // Caches the request
                mojangAccountNamesCache.add(playerName);
            } else {
                System.out.println("Player " + playerName + " doesn't have a Mojang account");
                // Player doesn't have a Mojang account
                this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                this.profile = new GameProfile(null, packet.name());
                ci.cancel();
            }
        } catch (IOException e) {
            e.printStackTrace();

            // Player probably doesn't have a Mojang account
            this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
            this.profile = new GameProfile(null, packet.name());
            ci.cancel();
        }
    }
}
