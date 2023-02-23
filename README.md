# YetAnotherAuthMod

YetAnotherAuthMod is a Minecraft mod that provides a simple authentication system for Minecraft servers.

# How to use
- Put the mod file in the server's mods folder.
- Set `online-mode` to `true` in server.properties
- Set `enforce-secure-profile` to `false` in server.properties
- Restart server

And that's it

# Features

- Session management: offline accounts only need to log in once every 30 days if they are connecting from the same IP and computer.
- Official Mojang authentication for premium accounts: original UUIDs are used for premium accounts so e.g. skins work properly.
- Limbo world: players are directed to a separate world to log in or register.

# Credits

Thanks to Patbox, creator of [Polymer](https://modrinth.com/mod/polymer) great server-side library, we are able to easily create the limbo world without the need for a second server or proxy.