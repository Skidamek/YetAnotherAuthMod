package pl.skidam.yetanotherauthmod;

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
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Function;

import static pl.skidam.yetanotherauthmod.yaam.mojangAccounts;

public class LimboHandler extends EarlyPlayNetworkHandler {
    private static final ArmorStandEntity FAKE_ENTITY = new ArmorStandEntity(EntityType.ARMOR_STAND, PolymerCommonUtils.getFakeWorld());
    private static final CommandDispatcher<LimboHandler> COMMANDS = new CommandDispatcher<>();
    private boolean registered;

    public LimboHandler(Context context) {
        super(new Identifier(yaam.MOD_ID), context);

        String playerName = this.getPlayer().getGameProfile().getName();
        String playerIP = Utils.extractContentInBrackets(this.getConnection().getAddress().toString());

        // If player is authenticated / has active session, join normal game
        if (yaam.sessions.checkSession(playerName, playerIP)) {
            this.sendPacket(new GameMessageS2CPacket(Text.literal("Authenticated using login session!").formatted(Formatting.GREEN), false));
            this.continueJoining();
            return;
        } else {
            if (mojangAccounts.contains(playerName.toLowerCase())) {
                this.sendPacket(new GameMessageS2CPacket(Text.literal("Authenticated using Mojang account!").formatted(Formatting.GREEN), false));
                this.continueJoining();
                return;
            }
        }

        registered = yaam.database.userExists(playerName);

        // Load into limbo
        sendToLimbo();

        this.sendPacket(new CommandTreeS2CPacket((RootCommandNode) COMMANDS.getRoot()));

        String username = String.valueOf(this.getPlayer().getEntityName());
        if (yaam.database.userExists(username)) {
            sendChatMessage("Welcome again " + username + "!", Formatting.GREEN);
            if (!yaam.sessions.activeSession(username)) {
                sendChatMessage("Your login session got expired!", Formatting.RED);
            }
        } else {
            sendChatMessage("Welcome " + username + "!", Formatting.GREEN);
        }
    }

    private void sendToLimbo() {
        this.sendInitialGameJoin();
        this.sendPacket(FAKE_ENTITY.createSpawnPacket());
        this.sendPacket(new EntityTrackerUpdateS2CPacket(FAKE_ENTITY.getId(), FAKE_ENTITY.getDataTracker().getChangedEntries()));
        this.sendPacket(new SetCameraEntityS2CPacket(FAKE_ENTITY));
        this.sendPacket(new CustomPayloadS2CPacket(CustomPayloadS2CPacket.BRAND, (new PacketByteBuf(Unpooled.buffer())).writeString("limbo")));
        this.sendPacket(new WorldTimeUpdateS2CPacket(0, 18000, false));
        this.sendPacket(new CloseScreenS2CPacket(0));
    }

    @Override
    public void onCommandExecution(CommandExecutionC2SPacket packet) {
        try {
            COMMANDS.execute(packet.command(), this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private int loginTime = 60 * 20 + 5; // 60 seconds (+ 5 ticks)
    protected void onTick() {
        if (loginTime == 0) {
            this.disconnect(Text.literal("Login timeout!").formatted(Formatting.RED));
            return;
        }

        if (loginTime % 20 == 0) {
            int seconds = loginTime / 20;
            sendActionBarMessage(String.valueOf(seconds), Formatting.RED);

            if (loginTime % 200 == 0) {
                if (registered) {
                    sendChatMessage("Login using /login <password>", Formatting.GREEN);
                } else {
                    sendChatMessage("Register using /register <password> <confirm_password>", Formatting.GREEN);
                }
            }
        }

        loginTime--;
    }

    private void sendChatMessage(String text, Formatting formatting) {
        Text message = Text.literal(text).formatted(formatting);
        this.sendPacket(new GameMessageS2CPacket(message, false));
    }

    private void sendActionBarMessage(String message, Formatting formatting) {
        Function<Text, Packet<?>> constructor = OverlayMessageS2CPacket::new;
        ServerCommandSource source = this.getPlayer().getCommandSource();
        try {
            this.sendPacket(
                    constructor.apply(
                            Texts.parse(
                                    source,
                                    Text.literal(message).formatted(formatting).formatted(Formatting.BOLD),
                                    this.getPlayer(),
                                    0
                            )
                    ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static LiteralArgumentBuilder<LimboHandler> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<LimboHandler, T> argument(String name, ArgumentType<T> argumentType) {
        return RequiredArgumentBuilder.argument(name, argumentType);
    }

    private int loginTries = 0;

    {
        FAKE_ENTITY.setPos(0, 64, 0);
        FAKE_ENTITY.setNoGravity(true);
        FAKE_ENTITY.setInvisible(true);
        var nbt = new NbtCompound();
        FAKE_ENTITY.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Marker", true);
        FAKE_ENTITY.readCustomDataFromNbt(nbt);

        String playerName = this.getPlayer().getGameProfile().getName();
        String playerIP = Utils.extractContentInBrackets(this.getConnection().getAddress().toString());

        if (registered) {

                COMMANDS.register(literal("register")
                        .then(argument("password", StringArgumentType.word())
                                .then(argument("confirm_password", StringArgumentType.word())
                                        .executes(x -> {
                                            String password = x.getArgument("password", String.class);
                                            String confirm = x.getArgument("confirm_password", String.class);
                                            if (password.equals(confirm) && !password.equals("")) {

                                                // add to login database and create session
                                                yaam.database.addUser(playerName, password);
                                                yaam.sessions.createSession(playerName, playerIP);

                                                this.disconnect(
                                                        Text.literal("Successfully registered!\n\n").formatted(Formatting.GREEN)
                                                                .append(Text.literal("Rejoin server to play!\n").formatted(Formatting.GREEN))
                                                );
                                            } else {
                                                sendChatMessage("Passwords don't match!", Formatting.RED);
                                            }
                                            return 0;
                                        })
                                )
                        )
                );

        } else {

            COMMANDS.register(literal("login")
                    .then(argument("password", StringArgumentType.word())
                            .executes(x -> {
                                String userInput = x.getArgument("password", String.class);
                                if (yaam.database.checkLogin(playerName, userInput)) {

                                    // create session
                                    yaam.sessions.createSession(playerName, playerIP);

                                    this.disconnect(
                                            Text.literal("Successfully created login session!\n\n").formatted(Formatting.GREEN)
                                                    .append(Text.literal("Rejoin server to play!\n").formatted(Formatting.GREEN))
                                    );

                                } else {
                                    loginTries++;
                                    if (loginTries >= 3) {
                                        this.disconnect(
                                                Text.literal("Too many login attempts!\n\n").formatted(Formatting.RED)
                                                        .append(Text.literal("Try again latter!\n").formatted(Formatting.RED))
                                        );
                                    }
                                    sendChatMessage("Incorrect password, attempt " + loginTries + "/3", Formatting.RED);
                                }
                                return 0;
                            })
                    )
            );
        }
    }
}
