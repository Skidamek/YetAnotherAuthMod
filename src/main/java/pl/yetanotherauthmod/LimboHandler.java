package pl.yetanotherauthmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import eu.pb4.polymer.networking.api.EarlyPlayNetworkHandler;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import static pl.yetanotherauthmod.yaam.MOD_ID;
import static pl.yetanotherauthmod.yaam.database;

public class LimboHandler extends EarlyPlayNetworkHandler {
    private static final ArmorStandEntity FAKE_ENTITY = new ArmorStandEntity(EntityType.ARMOR_STAND, PolymerCommonUtils.getFakeWorld());
    private static final CommandDispatcher<LimboHandler> COMMANDS = new CommandDispatcher<>();

    public LimboHandler(Context context) {
        super(new Identifier(MOD_ID), context);

        this.sendInitialGameJoin();

        this.sendPacket(FAKE_ENTITY.createSpawnPacket());
        this.sendPacket(new EntityTrackerUpdateS2CPacket(FAKE_ENTITY.getId(), FAKE_ENTITY.getDataTracker().getChangedEntries()));
        this.sendPacket(new SetCameraEntityS2CPacket(FAKE_ENTITY));
        this.sendPacket(new CustomPayloadS2CPacket(CustomPayloadS2CPacket.BRAND, (new PacketByteBuf(Unpooled.buffer())).writeString("limbo")));
        this.sendPacket(new WorldTimeUpdateS2CPacket(0, 18000, false));
        this.sendPacket(new CloseScreenS2CPacket(0));

        this.sendPacket(new CommandTreeS2CPacket((RootCommandNode) COMMANDS.getRoot()));

        String username = String.valueOf(this.getPlayer().getEntityName());
        if (yaam.database.userExists(username)) {
            this.sendPacket(new GameMessageS2CPacket(Text.literal("Login using /login password").formatted(Formatting.GREEN), false));
        } else {
            this.sendPacket(new GameMessageS2CPacket(Text.literal("Register using /register password confirm_password").formatted(Formatting.RED), false));
        }
    }

    @Override
    public void onCommandExecution(CommandExecutionC2SPacket packet) {
        try {
            COMMANDS.execute(packet.command(), this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static LiteralArgumentBuilder<LimboHandler> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<LimboHandler, T> argument(String name, ArgumentType<T> argumentType) {
        return RequiredArgumentBuilder.argument(name, argumentType);
    }

    {
        FAKE_ENTITY.setPos(0, 64, 0);
        FAKE_ENTITY.setNoGravity(true);
        FAKE_ENTITY.setInvisible(true);
        var nbt = new NbtCompound();
        FAKE_ENTITY.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Marker", true);
        FAKE_ENTITY.readCustomDataFromNbt(nbt);

        String username = String.valueOf(this.getPlayer().getEntityName());

        if (!yaam.database.userExists(username)) {
            COMMANDS.register(literal("register")
                    .then(argument("password", StringArgumentType.word())
                            .then(argument("confirm_password", StringArgumentType.word())
                                    .executes(x -> {
                                        String userInput1 = x.getArgument("password", String.class);
                                        String userInput2 = x.getArgument("confirm_password", String.class);
                                        if (userInput1.equals(userInput2) && !userInput1.equals("")) {
                                            String hashedPassword = Utils.sha512(userInput1);
                                            database.addUser(username, hashedPassword);
                                            this.sendPacket(new GameMessageS2CPacket(Text.literal("Successfully registered!").formatted(Formatting.GREEN), false));
                                            this.sendPacket(new GameMessageS2CPacket(Text.literal("Login using /login password").formatted(Formatting.GREEN), false));
                                        } else {
                                            this.sendPacket(new GameMessageS2CPacket(Text.literal("Passwords don't match!").formatted(Formatting.RED), false));
                                        }
                                        return 0;
                                    })
                            )
                    )
            );
        }

        COMMANDS.register(literal("login")
            .then(argument("password", StringArgumentType.word())
                .executes(x -> {
                    String userInput = x.getArgument("password", String.class);
                    String hashedPassword = Utils.sha512(userInput);
                    if (yaam.database.checkLogin(username, hashedPassword)) {
                        this.sendPacket(new GameMessageS2CPacket(Text.literal("Logged in!").formatted(Formatting.GREEN), false));
                        this.continueJoining();
                    } else {
                        this.sendPacket(new GameMessageS2CPacket(Text.literal("Wrong password!").formatted(Formatting.RED), false));
                    }
                    return 0;
                })
            )
        );
    }
}
