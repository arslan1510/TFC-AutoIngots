# TFC AutoIngots

Automatically generates TerraFirmaCraft ingot pile textures for ingots from any mod.

## Features

- Automatically detects ingots from any mod
- Generates TFC-style ingot pile textures using color extraction
- Zero configuration required

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.0+
- TerraFirmaCraft 4.0.0+ (required)

## Installation

1. Place the mod JAR in your `mods` folder
2. Launch the game - textures generate automatically at runtime

## How It Works

The mod scans for ingots tagged with `c:ingots` or items containing "ingot" in their name. It extracts the metal name from the item ID (e.g., `tfmg:lead_ingot` → `lead`) and generates a recolored version of TFC's ingot pile texture. Textures are generated in-memory at runtime and provided via a dynamic resource pack, so no manual resource pack selection is required.

## Building

```bash
./gradlew build
```

## License

MIT
