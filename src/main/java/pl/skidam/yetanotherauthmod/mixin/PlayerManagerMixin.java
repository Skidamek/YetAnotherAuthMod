package pl.skidam.yetanotherauthmod.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.skidam.yetanotherauthmod.TextHelper;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = PlayerManager.class, priority = 2137)
public class PlayerManagerMixin {

    @Inject(method = "createPlayer", at = @At("RETURN"))
    private void createPlayer(GameProfile profile, PlayerPublicKey publicKey, CallbackInfoReturnable<ServerPlayerEntity> cir) {

        List<ServerPlayerEntity> players = new ArrayList<>(((PlayerManager) (Object) this).getPlayerList());

        for (ServerPlayerEntity player : players) {
            if (player.getGameProfile().getName().equals(profile.getName())) {
                player.networkHandler.disconnect(TextHelper.translatable("multiplayer.disconnect.duplicate_login"));
            }
        }
    }
}
