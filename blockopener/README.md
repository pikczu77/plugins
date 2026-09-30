# Block Opener — „Minecraft, But You Can Open Any Block”

Mod Fabric odtwarzający mod z filmu [Henwy – *Minecraft, But You Can Open Any Block*](https://youtu.be/D1C58JjXpww)
(mod oryginalnie od Grasera). Każdy blok da się otworzyć **Otwieraczem Bloków**: większość ma w środku trochę
żelaza, rudy chowają łup ze skrzyń ze struktur, a **10 sekretnych bloków** ma w środku OP customowe przedmioty.

- Minecraft **1.21.11**, Fabric Loader **0.19.5+**, **Fabric API** (wymagane na serwerze i u graczy)
- Wszystkie tekstury są autorskie (pixel art generowany skryptem `tools/generate_textures.py`)
- Język gry: angielski i polski

| | |
|---|---|
| ![Otwieranie](docs/screenshots/opening.png) | ![Znaleziony przedmiot](docs/screenshots/found.png) |
| ![Showcase](docs/screenshots/showcase.png) | ![Tooltip](docs/screenshots/tooltip.png) |
| ![Zbroja](docs/screenshots/armor.png) | ![Woda Pustki](docs/screenshots/void_water.png) |

*(screenshoty robi automatycznie `./gradlew runClientGameTest`)*

## Otwieranie bloków

Weź Otwieracz Bloków (`/bo give`) i kliknij PPM dowolny blok. Tłok podnosi „wieko”, łup wyskakuje z góry,
a sekretny przedmiot najpierw unosi się, kręci i świeci. Pierwsze znalezienie każdego z 10 przedmiotów daje
tytuł na ekranie, dźwięk, wiadomość na czacie i zapala się ikonka w trackerze (lewy górny róg, klawisz **J**).

- **Kucnięcie + PPM** – normalne użycie bloku (skrzynia, stół rzemieślniczy…) z Otwieraczem w ręce.
- Skrzynie wysypują swoją zawartość, bloki zalane wodą zostawiają wodę.
- Nie da się otworzyć bloków technicznych, portali i ramki portalu Endu (żeby nie zablokować przejścia gry).
  Bedrock **da się** otworzyć (jak w filmie).

| Blok | Co jest w środku |
|---|---|
| Większość bloków | żelazo, czasem węgiel / złoto / nić / pochodnie / strzały |
| Ruda węgla | domek z wioski (dużo drewna) + szansa na złote jabłko |
| Ruda miedzi | skrzynia rybaka + domek z wioski (jedzenie!) |
| Ruda żelaza | kowal broni z wioski |
| Ruda złota | nagroda z komnat prób (wind charge, tarcza…) |
| Ruda redstone | korytarz twierdzy + perły Endu, obsydian, szmaragdy |
| Ruda lapis | skarb z wraku albo „diamentowy” łup |
| Ruda diamentów | zrujnowany portal / skarb bastionu, czasem perły Endu |
| Ruda szmaragdów | piramida pustynna |
| Blok żelaza / złota | 1 sztabka żelaza / 1 węgiel (żart z filmu) |

## 10 sekretnych przedmiotów

| # | Blok | Przedmiot | Moce |
|---|---|---|---|
| 1 | Dynia | **Dyniowe Buty** | sprint = ślad wybuchowych dyń; kucanie 3 s = zamiana w dynię (moby tracą cel) |
| 2 | Ul / gniazdo pszczół | **Pszczele Wiertło** | PPM = przywołaj pszczoły; PPM na moba (także z daleka) = pszczoły atakują; bronią cię i nie tracą żądła |
| 3 | Kowadło | **Kowadłowy Napierśnik** | skok + kucnięcie = wybuchowe uderzenie, które **otwiera wszystkie bloki dookoła** (deszcz łupu, szybkie kopanie w dół); kucanie = Odporność + spowolnienie |
| 4 | Blok diamentów | **Diamentowe Spodnie** | **M** = Diamentowy Lot; machnięcie pustą ręką w locie = kule ognia; Szybkość II |
| 5 | Blok nacieku | **Naciekowy Miecz** | 9 obrażeń; kucnięcie + atak = Deszcz Stalaktytów; PPM = deszcz tam, gdzie patrzysz |
| 6 | Tłok | **Tłokowa Wyrzutnia** | kucnięcie + PPM = turbo wystrzał; PPM na moba = mega kopniak (wybucha przy uderzeniu); PPM = działo z TNT |
| 7 | Blok miedzi | **Miedziany Magnes** | w ręce przyciąga przedmioty; PPM = rozbrojenie wszystkich w okolicy; kucanie = turbo przyciąganie |
| 8 | Bedrock | **Wiadro z Bedrocka** | nieskończona Woda Pustki (pożera przedmioty, rani przez zbroję); kucnięcie + PPM = wypompowanie |
| 9 | Wrzeszczak / katalizator sculk | **Sculkowy Hełm** | **G** = Sonic Boom Wardena (przez ściany); odporność na Ciemność; czujniki sculk cię nie słyszą |
| 10 | Blok mchu | **Mchowa Kula** | rzucana, zabija wszystko, w co trafi (smoka też!); tam, gdzie spadnie, rośnie mech |

Własne wybuchy nigdy nie ranią właściciela. Jak zwykłe TNT niszczą leżące przedmioty (wyłączysz: `/bo settings explosionsDestroyItems false`); wyjątkiem jest uderzenie kowadła, które zostawia łup z otwartych bloków. Customowe przedmioty są niezniszczalne.
Moce w tooltipie są ukryte pod „Przytrzymaj SHIFT”, żeby nie spoilerować na nagraniu.

## Klawisze (do zmiany w Opcje → Sterowanie → Block Opener)

| Klawisz | Akcja |
|---|---|
| **M** | Diamentowy Lot (w Diamentowych Spodniach) |
| **G** | Sonic Boom (w Sculkowym Hełmie) |
| **J** | pokaż / ukryj tracker 10 przedmiotów |

## Komendy do nagrywania (`/bo` = `/blockopener`)

| Komenda | Po co |
|---|---|
| `/bo give [gracze] [opener\|all\|<przedmiot>]` | daje Otwieracz, wszystko albo jeden przedmiot |
| `/bo reset [gracze]` | zeruje znalezione przedmioty – szybki dubel ujęcia |
| `/bo complete [gracze]` | oznacza wszystkie 10 jako znalezione (pełny tracker) |
| `/bo reveal <gracz> <przedmiot>` | odgrywa moment „CUSTOMOWY PRZEDMIOT!” i daje przedmiot |
| `/bo timer start <min> [sek]`, `pause`, `stop`, `add <sek>` | boss bar z odliczaniem (np. „20 minut, potem walka”) |
| `/bo locate <przedmiot>` | podświetla przez ściany najbliższy blok z tym przedmiotem (klik w koordy = /tp) |
| `/bo testrow` | stawia przed tobą rząd 10 sekretnych bloków |
| `/bo showcase [sekundy]`, `/bo showcase clear` | 10 przedmiotów lewituje i kręci się przed tobą (ujęcie jak intro filmu) |
| `/bo hud [true\|false] [gracze]` | tracker dla wybranych graczy |
| `/bo progress [gracz]` | lista znalezionych przedmiotów |
| `/bo open <x y z>` | otwiera blok komendą (np. z bloku poleceń do cinematiców) |
| `/bo settings …` | `explosionsBreakBlocks`, `explosionsDestroyItems`, `announceFinds`, `lootRolls` (1-16, więcej łupu), `openCooldown` |

## Dostosowanie

Zawartość bloków to zwykłe tabele łupu: `data/blockopener/loot_table/opening/<namespace>/<blok>.json`
(domyślna: `opening/default.json`). Datapackiem możesz zmienić, co jest w dowolnym bloku, albo ukryć
sekretny przedmiot w innym bloku. Bloki, których nie da się otworzyć: tag `blockopener:unopenable`.

## Budowanie

Gradle / Loom 1.18 wymaga **Javy 25** do uruchomienia builda (sam mod jest kompilowany pod Javę 21).

```bash
./gradlew build              # jar: build/libs/blockopener-1.21.11-1.0.0.jar, odpala też testy serwerowe
./gradlew runClient          # gra z modem
./gradlew runClientGameTest  # test klienta, robi screenshoty scen do build/run/clientGameTest/screenshots
python3 tools/generate_textures.py && python3 tools/generate_data.py   # przegenerowanie zasobów
```
