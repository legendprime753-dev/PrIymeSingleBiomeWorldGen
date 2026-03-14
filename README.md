# PrIymeSingleBiomeWorldGen

Ein PaperMC/Purpur-Plugin für Minecraft **1.21.x**, mit dem du schnell performante **Single-Biome-Welten** erstellen und verwalten kannst.

Das Plugin erstellt bewusst reduzierte Welten (kein Vanilla-Overhead durch Caves/Structures/Decorations), damit Worldgen stabil und kontrollierbar bleibt.

---

## Features

- Erstellen von Welten mit **genau einem Biome**
- Zwei Generierungsmodi:
  - **FLAT** (flach)
  - **NOISE** (sanfte Hügel)
- Konfigurierbare Baum-Generierung pro Chunk
- Preset-System (speichern/laden/löschen)
- GUI-gestützte Welterstellung (`/sbw gui`)
- Automatisches Laden verwalteter Welten beim Serverstart
- Optionales Unterdrücken natürlicher Mob-Spawns in Plugin-Welten

---

## Voraussetzungen

- Java **21**
- Paper oder Purpur **1.21.x**
- Maven (nur wenn du selbst bauen willst)

---

## Build (aus Source)

```bash
mvn clean package
```

Die JAR liegt danach unter:

```text
target/singlebiome-worldgen-1.0.2.jar
```

---

## Installation

1. Server stoppen
2. JAR nach `plugins/` kopieren
3. Server starten
4. Optional: `plugins/PrIymeSingleBiomeWorldGen/config.yml` anpassen
5. Server neu starten oder `/sbw reload`

---

## Befehle

### Basis

- `/sbw gui` – Öffnet den World-Creator
- `/sbw list` – Zeigt alle verwalteten Plugin-Welten
- `/sbw info <world>` – Zeigt gespeicherte Konfiguration
- `/sbw load <world>` – Lädt eine gespeicherte Welt
- `/sbw delete <world>` – Löscht eine verwaltete Welt
- `/sbw go <world>` – Teleportiert in eine Welt
- `/sbw reload` – Lädt Plugin-Config neu

### Welterstellung

- `/sbw create <world> <biome> <flat|noise> [treesPerChunk|on|off] [seed]`
- `/sbw create <world> preset <presetName> [seed]`

Beispiele:

```text
/sbw create test minecraft:plains flat 0
/sbw create test minecraft:plains noise 8 12345
/sbw create nether_single minecraft:nether_wastes noise off random
```

### Presets

- `/sbw preset list`
- `/sbw preset save <presetName> <biome> <flat|noise> [treesPerChunk|on|off] [seed]`
- `/sbw preset savefrom <presetName> <world>`
- `/sbw preset load <presetName> <worldName>`
- `/sbw preset delete <presetName>`

---

## Rechte (Permissions)

- `priyme.sbw.create` – Welten erstellen/laden
- `priyme.sbw.delete` – Welten löschen
- `priyme.sbw.preset` – Presets verwalten
- `priyme.sbw.tp` – Teleport (`/sbw go`)

---

## Konfiguration (Kurzüberblick)

Datei: `plugins/PrIymeSingleBiomeWorldGen/config.yml`

Wichtige Bereiche:

- `biome-whitelist` – Erlaubte Biome einschränken
- `defaults.mode` – Standardmodus (`NOISE` oder `FLAT`)
- `defaults.flat.*` – Höhe/Filler für Flat
- `defaults.noise.*` – Basisparameter für Noise
- `defaults.trees.*` – Baum-Defaults
  - `per-chunk`: **0..24**
- `mobs.suppress-natural-spawning` – Natürliches Spawnen unterdrücken
- `worlds.auto-load-on-startup` – Welten beim Start automatisch laden

---

## Datenablage

- Welt-Konfigurationen:  
  `plugins/PrIymeSingleBiomeWorldGen/worlds/<world>.yml`
- Presets:  
  `plugins/PrIymeSingleBiomeWorldGen/presets/<name>.yml`

---

## Troubleshooting

### Welt wird nicht erstellt

- Prüfe, ob das Biome durch `biome-whitelist` erlaubt ist
- Prüfe, ob der Weltname bereits existiert
- Prüfe Server-Logs auf Fehler beim Weltladen

### Hohe Last bei Worldgen

- `trees.per-chunk` reduzieren
- `NOISE`-Parameter moderat halten
- Große Pre-Generation auf mehrere Schritte verteilen

### Befehle funktionieren nicht

- Rechte prüfen (Permissions)
- Plugin beim Start korrekt geladen?
- `/sbw reload` nach Config-Änderungen ausführen

---

## Kompatibilität

Getestet/ausgelegt für **Paper/Purpur 1.21.x**.
Bei anderen Forks oder Versionen kann es API-Abweichungen geben.
