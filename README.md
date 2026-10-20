# Donut Client (Fabric 1.21.11)

Client-Mod mit ClickGUI und zwei Modulen:

- **StorageFinder** – hebt Kisten, Shulker und Dropper hervor (optional: Trapped Chests, Fässer, Ender-Kisten, Dispenser, Trichter)
- **SpawnerFinder** – hebt Monster- und Trial-Spawner hervor

## Bedienung

- **Rechte Shift-Taste** öffnet das GUI (Taste unter Optionen → Steuerung → "Donut Client" änderbar)
- Modul an/aus: Schalter rechts in der Karte · Stern = Favorit · **Configure >** öffnet die Einstellungen
- Tab **Appearance**: Akzentfarbe, Fensterdeckkraft, Rundung, Animationen
- Alle Änderungen werden automatisch in `config/donutclient.json` gespeichert

## Einstellungen pro Finder-Modul

| Bereich | Einstellung |
|---|---|
| Targets | jeden Blocktyp einzeln an/aus, jeweils mit eigener Farbe |
| Rendering | Render Mode (Outline / Fill / Outline + Fill), Visibility (Through Walls / Visible Only), Outline Opacity, Fill Opacity, Outline Thickness, Box Padding, Distance Fade |
| Colors | Color Mode (Per Type / Single Color / Rainbow), Single Color, Rainbow Speed |
| Scanning | Range (16–512 Blöcke), Max Highlights |

Es werden nur bereits geladene Chunks durchsucht (Render Distance des Servers/Spiels begrenzt die Reichweite).

## Jar bauen (ohne lokales JDK, über GitHub)

1. Auf GitHub ein neues Repository anlegen.
2. ZIP entpacken und **alle Dateien und Ordner** hochladen (auch den versteckten Ordner `.github`, sonst läuft der Build nicht automatisch).
3. Tab **Actions** → Workflow **build** → fertigen Lauf öffnen → unten bei **Artifacts** auf **DonutClient** klicken.
4. Die `.jar` aus dem Download in den `mods`-Ordner legen, zusammen mit **Fabric Loader** und **Fabric API** für 1.21.11.

Lokal baut es mit JDK 21 per `./gradlew build` – die Jar liegt danach in `build/libs/`.
