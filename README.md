# YetAnotherAuthMod

YetAnotherAuthMod is a mod for Minecraft that adds a simple authentication system to the game.

# How to use
- put in server mods folder
- set `online-mode` to `true` in server.properties
- set `enforce-secure-profile` to `false` in server.properties
- restart server

that's it
# Features
- Sessions (Offline accounts needs to log in only once in 30 days if they are connecting from the same IP and computer)
- Official Mojang Auth for premium accounts
- Limbo world to login/register

#

Thanks to [Polymer](https://modrinth.com/mod/polymer) for creating great networking library to be able to easily create fake limbo world without needing second server or proxy.