# Jak wgrać HP Size na Modrinth

Wszystko, co trzeba wkleić, jest w tym folderze (`hpsize/modrinth/`). Plik moda to `hpsize-1.0.0+1.21.11.jar`
(z czatu albo z GitHub Actions → workflow `hpsize` → artefakt `hpsize-mod`).

## 1. Utwórz projekt

[modrinth.com](https://modrinth.com) → zaloguj się → **Create a project** (ikona `+` na górze):

| Pole | Wpisz |
|---|---|
| Project type | **Mod** |
| Name | `HP Size` |
| URL | `hpsize` (wtedy link to modrinth.com/mod/hpsize; tak jest ustawione w modzie) |
| Visibility | Public |
| Summary | treść pliku `summary.txt` |

## 2. Uzupełnij stronę projektu (Settings)

| Zakładka | Co ustawić |
|---|---|
| **General → Icon** | `icon.png` |
| **Description** | wklej całą treść `description.md` |
| **Tags** | Game Mechanics, Mobs, Utility (zaznacz wszystkie trzy jako wyróżnione) |
| **Environments** | serwer: **wymagany**, klient: **opcjonalny** (działa w singleplayer i na serwerze, gracze nie muszą mieć moda) |
| **License** | MIT |
| **Links** | Source: `https://github.com/pikczu77/plugins`, Issues: `https://github.com/pikczu77/plugins/issues` |
| **Gallery** | pliki z `gallery/`, tytuły i opisy niżej |

### Galeria

| Plik | Title | Description |
|---|---|---|
| `gallery-1-lineup.png` (zaznacz **Featured**) | Size = health | From a 3 HP rabbit to a 100 HP iron golem: 20 HP is normal size. |
| `gallery-2-giant.png` | The biggest | A Warden with 500 HP hits the game's size limit (×16). |
| `gallery-3-shrinking.png` | Every hit shrinks | Iron golems with 100, 50, 20 and 5 HP. |
| `gallery-4-countdown.png` | Recording tools | /go countdown with the clean HUD (H). |

## 3. Wgraj wersję (Versions → Create a version)

| Pole | Wpisz |
|---|---|
| File | `hpsize-1.0.0+1.21.11.jar` |
| Version name | `HP Size 1.0.0` |
| Version number | `1.0.0+1.21.11` |
| Release channel | Release |
| Loaders | Fabric |
| Game versions | 1.21.11 |
| Dependencies | **Fabric API** → Required (wyszukaj „Fabric API”) |
| Changelog | treść `changelog.md` |

Modrinth zwykle sam wypełnia loader i wersję gry z pliku `.jar`, sprawdź tylko, czy się zgadzają.

## 4. Wyślij do sprawdzenia

**Submit for review**. Moderatorzy Modrinth zwykle zatwierdzają nowy projekt w ciągu kilku dni.
Do tego czasu projekt jest widoczny tylko dla Ciebie.

## Kolejne wersje

Zmień `version` w `gradle.properties` (np. `1.0.1+1.21.11`), zbuduj (`./gradlew build` albo GitHub Actions),
a na Modrinth dodaj nową wersję w zakładce **Versions** z nowym plikiem `.jar`.
