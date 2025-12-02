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
2. Launch the game - textures generate automatically
3. Enable `tfcautoingots_generated` resource pack in Options > Resource Packs
4. Restart the game

## How It Works

The mod scans for ingots tagged with `c:ingots` or items containing "ingot" in their name. It extracts the metal name from the item ID (e.g., `tfmg:lead_ingot` → `lead`) and generates a recolored version of TFC's ingot pile texture. Textures are saved to `resourcepacks/tfcautoingots_generated/` and must be enabled manually.

## Building

```bash
./gradlew build
```

## License

MIT
