# RecKit — baza przedmiotów do filmów

Mod Fabric na **Minecraft 1.21.11**: biblioteka customowych przedmiotów wyciągniętych z datapacków i paczek tekstur.
Z każdej paczki bierzemy **tylko przedmioty** (tekstury, modele, nazwy) — bez mechanik, crafingów i trybu gry.
Każdy przedmiot to zwykły rekwizyt: można go trzymać w ręce, położyć w ramce, wyrzucić, ale nic nie robi.

## Instalacja

1. Zainstaluj [Fabric Loader](https://fabricmc.net/use/installer/) dla 1.21.11 (0.17.3 lub nowszy).
2. Wrzuć do folderu `mods`:
   - `reckit-1.21.11-1.0.0.jar` (ten mod),
   - [Fabric API](https://modrinth.com/mod/fabric-api) dla 1.21.11.

Na serwerze mod musi być **na serwerze i u wszystkich graczy** (przedmioty są zarejestrowane, nie podmieniają vanilli).

## Użycie

- **Tryb kreatywny**: każda paczka ma swoją zakładkę w ekwipunku kreatywnym (strzałki na górze, jeśli zakładek jest dużo).
  Przedmioty znajdziesz też w wyszukiwarce.
- **Komenda**: `/give @s reckit:<id>`, np. `/give @s reckit:ruby_sword` (Tab podpowiada nazwy).

## Przedmioty

Jeszcze żadnych — dochodzą z kolejnymi paczkami.

## Jak to jest zbudowane

| Plik | Co zawiera |
|---|---|
| `src/main/resources/reckit/catalog.json` | lista paczek i przedmiotów (kolejność = kolejność w zakładce) |
| `src/main/resources/assets/reckit/items/<id>.json` | definicja modelu przedmiotu (format 1.21.4+) |
| `src/main/resources/assets/reckit/models/item/<paczka>/` | modele przedmiotów danej paczki |
| `src/main/resources/assets/reckit/textures/item/<paczka>/` | tekstury danej paczki (animowane z `.mcmeta`) |
| `src/main/resources/assets/reckit/lang/pl_pl.json`, `en_us.json` | nazwy przedmiotów i zakładek |

Wpis w katalogu:

```json
{"id": "ruby_sword", "stack": 1, "glint": true, "color": "#FF5555"}
```

- `id` — nazwa w `/give` (`reckit:ruby_sword`), unikalna w całym modzie,
- `stack` — ile mieści się w jednym slocie (domyślnie 64),
- `glint` — zawsze świeci jak zaklęty (domyślnie nie),
- `color` — kolor nazwy (domyślnie biały).

`python3 tools/check_assets.py` (także w `./gradlew build`) sprawdza, czy każdy przedmiot ma definicję, modele,
tekstury i nazwy w obu językach. `./gradlew runClientGameTest` uruchamia prawdziwego klienta i robi zrzuty ekranu
każdej zakładki i każdego przedmiotu w ręce (pierwsza i trzecia osoba) do `build/run/clientGameTest/screenshots`.

## Licencja

Kod moda: MIT. Tekstury i modele przedmiotów należą do autorów paczek, z których pochodzą (lista wyżej).
