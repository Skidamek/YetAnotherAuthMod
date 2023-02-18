package pl.yetanotherauthmod;

import eu.pb4.polymer.networking.api.EarlyPlayNetworkHandler;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static pl.yetanotherauthmod.yaam.LOGGER;
import static pl.yetanotherauthmod.yaam.MOD_ID;

public class LimboHandler extends EarlyPlayNetworkHandler {
    private static final ArmorStandEntity FAKE_ENTITY = new ArmorStandEntity(EntityType.ARMOR_STAND, PolymerCommonUtils.getFakeWorld());

    public LimboHandler(Context context) {
        super(new Identifier(MOD_ID), context);

        ServerPlayerEntity player = this.getPlayer();

        this.sendInitialGameJoin();

        this.sendPacket(FAKE_ENTITY.createSpawnPacket());
        this.sendPacket(new EntityTrackerUpdateS2CPacket(FAKE_ENTITY.getId(), FAKE_ENTITY.getDataTracker().getChangedEntries()));
        this.sendPacket(new SetCameraEntityS2CPacket(FAKE_ENTITY));
        this.sendPacket(new CustomPayloadS2CPacket(CustomPayloadS2CPacket.BRAND, (new PacketByteBuf(Unpooled.buffer())).writeString(this.getServer().getServerModName() + "/" + "limbo")));
        this.sendPacket(new WorldTimeUpdateS2CPacket(0, 18000, false));

        LOGGER.info("Player " + player.getEntityName() + " joined to limbo!");
    }

    static {
        {
            FAKE_ENTITY.setPos(0, 64, 0);
            FAKE_ENTITY.setNoGravity(true);
            FAKE_ENTITY.setInvisible(true);
            var nbt = new NbtCompound();
            FAKE_ENTITY.writeCustomDataToNbt(nbt);
            nbt.putBoolean("Marker", true);
            FAKE_ENTITY.readCustomDataFromNbt(nbt);
        }
    }
}
