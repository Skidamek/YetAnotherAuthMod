package pl.skidam.yetanotherauthmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import eu.pb4.polymer.api.utils.PolymerUtils;
import eu.pb4.polymer.api.x.EarlyPlayNetworkHandler;
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
import net.minecraft.world.GameMode;

import java.util.Optional;
import java.util.function.Function;

import static pl.skidam.yetanotherauthmod.yaam.LOGGER;

public class LimboHandler extends EarlyPlayNetworkHandler {
    private static final ArmorStandEntity FAKE_ENTITY = new ArmorStandEntity(EntityType.ARMOR_STAND, PolymerUtils.getFakeWorld());
    private static final CommandDispatcher<LimboHandler> COMMANDS = new CommandDispatcher<>();
    private static Text cyclingText;

    @SuppressWarnings("unchecked")
    public LimboHandler(Context context) {
        super(new Identifier(yaam.MOD_ID), context);

        String playerName = this.getPlayer().getGameProfile().getName();
        String playerIP = Utils.formatIP(this.getConnection().getAddress().toString());
        String playerUUID = this.getPlayer().getUuidAsString().replace("-", "").toLowerCase();

        // Check if player is authenticated using Mojang account if so join normal game
        if (yaam.database.checkLogin(playerUUID, null)) {

            if (yaam.database.userExists(playerName)) {
                LOGGER.warn("{} likely bought original mojang account or original premium user owner logged in.", playerName);
                sendChatMessage("You likely bought original mojang account or original premium user owner logged in.", Formatting.YELLOW);
                sendChatMessage("Please contact server administrator. If you want to transfer your inventory and etc.", Formatting.YELLOW);
                sendChatMessage("Now no one can login on this username without mojang authentication.", Formatting.GREEN);

                yaam.database.removeUser(playerName);
                yaam.sessions.deleteSession(playerName);

                // TODO: handle this case that's quite hard
                // Player bought original mojang account or original premium user owner logged in.
                // Let's give player choice if they want to use fresh account or old one from non-premium login.
                // If player choose to use fresh account, we will remove old one from minecraft files and database.
                // If player choose old account, we need to change files from non-premium uuid to premium uuid.

                // There are quite a lot of things that can go wrong e.g. compatibility with other mods.
                // Other mods can use non-premium uuid to store player data...
            }

            sendChatMessage("Authenticated using Mojang account!", Formatting.GREEN);
            this.continueJoining();
            return;
        }

        // Check if player has active session if so join normal game
        if (yaam.sessions.checkSession(playerName, playerIP)) {
            if (!yaam.sessions.moreSessionsOnThisIP(playerIP)) {
                sendChatMessage("Authenticated using login session!", Formatting.GREEN);
                this.continueJoining();
                return;
            }
        }

        // Load into limbo
        var player = this.getPlayer();
        var server = this.getServer();
        this.sendPacket(new GameJoinS2CPacket(player.getId(), false, GameMode.SPECTATOR, null, server.getWorldRegistryKeys(), server.getRegistryManager(), server.getOverworld().getDimensionKey(), server.getOverworld().getRegistryKey(), 0, server.getPlayerManager().getMaxPlayerCount(), 2, 2, false, false, false, true, Optional.empty()));

        this.sendPacket(FAKE_ENTITY.createSpawnPacket());
        this.sendPacket(new EntityTrackerUpdateS2CPacket(FAKE_ENTITY.getId(), FAKE_ENTITY.getDataTracker(), true));
        this.sendPacket(new SetCameraEntityS2CPacket(FAKE_ENTITY));
        this.sendPacket(new CustomPayloadS2CPacket(CustomPayloadS2CPacket.BRAND, (new PacketByteBuf(Unpooled.buffer())).writeString("limbo")));
        this.sendPacket(new WorldTimeUpdateS2CPacket(0, 18000, false));
        this.sendPacket(new CloseScreenS2CPacket(0));

        String username = String.valueOf(this.getPlayer().getEntityName());

        if (yaam.database.userExists(username)) {

            sendChatMessage("Welcome again " + username + "!", Formatting.GREEN);
            registerCommands(true);

            cyclingText = TextHelper.literal("Login using /login <password>").formatted(Formatting.GREEN);

            if (!yaam.sessions.activeSession(username)) {
                sendChatMessage("Your login session got expired!", Formatting.RED);
            } else if (yaam.sessions.moreSessionsOnThisIP(playerIP)) {
                sendChatMessage("You have active session but there are more players playing from the same IP, you need to login manually.", Formatting.YELLOW);
            } else {
                sendChatMessage("Looks like you are logging from different IP address, login again to activate new session!", Formatting.GREEN);
            }
        } else {
            sendChatMessage("Welcome " + username + "!", Formatting.GREEN);
            registerCommands(false);

            cyclingText = TextHelper.literal("Register using /register <password> <confirm_password>").formatted(Formatting.GREEN);
        }

        this.sendPacket(new CommandTreeS2CPacket((RootCommandNode) COMMANDS.getRoot()));
    }

    @Override
    public void onCommandExecution(CommandExecutionC2SPacket packet) {
        try {
            COMMANDS.execute(packet.command(), this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private int loginTime = 60 * 20; // 60 seconds
    private int waitTime = 5;
    @Override
    public void tick() {
        if (waitTime > 0) {
            waitTime--;
            return;
        }

        if (loginTime == 0) {
            this.disconnect(TextHelper.literal("Login timeout!").formatted(Formatting.RED));
            return;
        }

        if (loginTime % 20 == 0) {
            int seconds = loginTime / 20;
            sendActionBarMessage(String.valueOf(seconds), Formatting.RED);

            if (loginTime % 200 == 0) {
                // send chat message
                this.sendPacket(new GameMessageS2CPacket(cyclingText, false));
            }
        }

        loginTime--;
    }

    private void sendChatMessage(String text, Formatting formatting) {
        Text message = TextHelper.literal(text).formatted(formatting);
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
                                    TextHelper.literal(message).formatted(formatting).formatted(Formatting.BOLD),
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


    static {
        FAKE_ENTITY.setPos(0, 64, 0);
        FAKE_ENTITY.setNoGravity(true);
        FAKE_ENTITY.setInvisible(true);
        var nbt = new NbtCompound();
        FAKE_ENTITY.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Marker", true);
        FAKE_ENTITY.readCustomDataFromNbt(nbt);
    }


    private int loginTries = 0;

    private void registerCommands(boolean playerAlreadyRegistered) {
        String playerName = this.getPlayer().getGameProfile().getName();
        String playerIP = Utils.formatIP(this.getConnection().getAddress().toString());

        if (playerAlreadyRegistered) {

            COMMANDS.register(literal("login")
                    .then(argument("password", StringArgumentType.word())
                            .executes(x -> {
                                String userInput = x.getArgument("password", String.class);
                                if (yaam.database.checkLogin(playerName, userInput)) {

                                    // create session
                                    yaam.sessions.createSession(playerName, playerIP);

                                    if (yaam.sessions.moreSessionsOnThisIP(playerIP)) {
                                        sendChatMessage("Logged in!", Formatting.GREEN);
                                        continueJoining();
                                        return 0;
                                    }

                                    this.disconnect(
                                            TextHelper.literal("Successfully created login session!\n").formatted(Formatting.GREEN)
                                                    .append(TextHelper.literal("Rejoin server to play!").formatted(Formatting.GREEN))
                                    );

                                } else {
                                    loginTries++;
                                    if (loginTries >= 3) {
                                        this.disconnect(
                                                TextHelper.literal("Too many login attempts!\n").formatted(Formatting.RED)
                                                        .append(TextHelper.literal("Try again latter!").formatted(Formatting.RED))
                                        );
                                    }
                                    sendChatMessage("Incorrect password, attempt " + loginTries + "/3", Formatting.RED);
                                }

                                return 0;
                            })
                    )
            );


        } else {

            COMMANDS.register(literal("register")
                    .then(argument("password", StringArgumentType.word())
                            .then(argument("confirm_password", StringArgumentType.word())
                                    .executes(x -> {
                                        String password = x.getArgument("password", String.class);
                                        String confirm = x.getArgument("confirm_password", String.class);

                                        if (!password.equals(confirm) || password.equals("")) {
                                            sendChatMessage("Passwords don't match!", Formatting.RED);
                                            return 0;
                                        }

                                        if (password.length() >= 6) {
                                            // add to login database and create session
                                            yaam.database.addUser(playerName, password);
                                            yaam.sessions.createSession(playerName, playerIP);

                                            this.disconnect(
                                                    TextHelper.literal("Successfully registered!\n").formatted(Formatting.GREEN)
                                                            .append(TextHelper.literal("Rejoin server to play!").formatted(Formatting.GREEN))
                                            );
                                        } else {
                                            sendChatMessage("Password need to contain at least 6 characters!", Formatting.RED);
                                        }
                                        return 0;
                                    })
                            )
                    )
            );
        }
    }
}
