# OSRS Coach Bank Exporter — developer prototype

This is **Java source code**, not an installable RuneLite Plugin Hub release or a compiled JAR. It has not been compiled or tested against a live RuneLite client. **Do not drop this source ZIP into your RuneLite plugins folder.**

## What it does

When RuneLite emits a BANK `ItemContainerChanged` event while logged in, the plugin captures positive-quantity non-placeholder bank items and writes `bank.json` to:

`%USERPROFILE%\.runelite\character-exporter\<character name>\bank.json`

The format matches the Python coach V11.1 importer: `schema_version`, `account_name`, UTC `exported_at`, and `items` (`id`, `quantity`). It does not transmit anything, access passwords or PINs, or perform game inputs. It writes a point-in-time snapshot only when the bank container changes; **merely opening the bank may not always trigger the event**.

## Building / testing

1. Install a JDK and Gradle (the ZIP does not include a Gradle wrapper).
2. From this directory run `gradle build` with network access to RuneLite Maven.
3. For RuneLite development, load the plugin in a compatible development RuneLite project and validate compilation, lifecycle and bank events. Check RuneLite's current third-party plugin policy and file-write restrictions before distribution.
4. Test with a non-sensitive account and compare exported item quantities to the visible bank. Confirm the bank.json path matches the selected Character Export folder in the coach.
5. Only after a verified RuneLite-compatible build should the plugin be submitted to Plugin Hub review, if allowed.

**Known limitations:** Character Export may normalise character folder names differently; confirm paths. A character name rename can change the output folder. Bank snapshots become stale after offline changes or until another bank event. Other containers (seed vault, group storage, inventory) are not included. RuneLite Plugin Hub review may prohibit or constrain direct filesystem writes. The source is a prototype, not a guaranteed approved plugin.
