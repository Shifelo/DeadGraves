# DeadGraves (Spigot 1.21.4)

> [!WARNING]
>This is a sample code project meant for demonstration and knowledge sharing. Not all features, checks, and protections required for a real server plugin are included. Not ‘production-ready’. Use at your own risk!

## Description

DeadGraves is a Spigot plugin for Minecraft 1.21.4. Whenever a player dies, DeadGraves will build a player head at the place of its death; hence the grave. This grave safely keeps all the dead player’s items.

The player can interact with the graves to get their stuff back. 
Project initially aims at demonstrating:
- Handling events with Spigot API (like player’s death, block interaction, and inventory events)
- And, also managing and manipulating inventories.
- Storing simple data with SQLite. 
- Exploring main Java ideas while making Minecraft plugins.

## Features
- **Auto Grave Creation**: The most important thing for players is upon his death, to appear a head with all his distributed items
- **Persistent Storage**: Using SQLite, the graves as well as the items inside them can be persistently stored.
- **Graves Interaction**:
    - Left Click opens inventory for the grave
    - Right Click attempts to auto equip armor and puts items into player inventory
 -   **Destruction of the Grave**: Breaking the head will pour out what’s inside.
-   **Automated Cleaning**: Empty graves are removed automatically.


## License

This code is provided as it is for educational purposes. You may modify it, use it to learn from it.

---

**Author:** Shifelo

> _This project is a sample to demonstrate knowledge of Spigot plugin development, custom enchantments, and Java best practices. It is not a finished product._
