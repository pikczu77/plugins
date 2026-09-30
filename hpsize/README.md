# HP Size — moby są tak duże, jak dużo mają życia

Mod Fabric na **Minecraft 1.21.11** zrobiony na podstawie filmu
[„Minecraft, but Mobs are as big as their health”](https://youtu.be/75mSpDQvTHo) (stuhpy).
Rozmiar każdego moba zależy od jego HP: kurczak (4 HP) jest 4× większy, krowa (10 HP) 10×,
Warden czy Wither dochodzą do limitu gry (16×). Każde uderzenie zmniejsza moba.

Do tego komendy, które ułatwiają nagrywanie odcinka i poprawiają film dla widza.

## Instalacja

1. Zainstaluj [Fabric Loader](https://fabricmc.net/use/installer/) dla 1.21.11 (0.17.3 lub nowszy).
2. Wrzuć do folderu `mods`:
   - `hpsize-1.21.11-1.0.0.jar` (ten mod),
   - [Fabric API](https://modrinth.com/mod/fabric-api) dla 1.21.11.
3. W świecie potrzebujesz uprawnień do komend (singleplayer: „Zezwól na kody” / Otwórz dla LAN → kody włączone).

Mod działa też na serwerze. Gracze bez moda mogą wejść (widzą giganty, paski, odliczania),
a klawisze czystego HUD i zoomu działają tylko u osób, które mają moda u siebie.

## Mechanika z filmu — `/hpsize`

| Komenda | Co robi |
|---|---|
| `/hpsize` | pokazuje wszystkie ustawienia |
| `/hpsize on` / `off` | włącza / wyłącza mechanikę (po wyłączeniu wszystko wraca do normy) |
| `/hpsize preset film` | **dokładnie jak w filmie**: rozmiar = HP, zmiana natychmiastowa |
| `/hpsize preset smooth` | jak w filmie, ale moby płynnie „oklapują” po ciosie (domyślne) |
| `/hpsize preset fair` | każdy cios zmniejsza moba — także bossów (w filmie Ravager i Wither się nie zmniejszali, bo miały więcej niż 16 HP) |
| `/hpsize preset light` | mniejsi giganci (maks. ×6) — mniej lagów |
| `/hpsize mode health\|max\|percent\|sqrt` | wzór: aktualne HP / maks. HP / maks. HP × % życia / pierwiastek z HP |
| `/hpsize factor <liczba>` | mnożnik (rozmiar = HP × mnożnik) |
| `/hpsize min <x>` / `max <x>` | najmniejszy / największy rozmiar (0.0625–16) |
| `/hpsize smooth <0-100>` | płynność zmian (0 = natychmiast) |
| `/hpsize players on\|off` | czy gracze też zmieniają rozmiar (pomysł na kolejny odcinek!) |
| `/hpsize suffocation on\|off` | czy giganci duszą się w blokach (w filmie przez to malały w jaskiniach) |
| `/hpsize exclude <mob>` / `include <mob>` | mob zachowa normalny rozmiar (np. `minecraft:enderman` na walkę ze smokiem) |
| `/hpsize freeze` / `unfreeze` | zamraża rozmiary (ujęcie bez zmian) |
| `/hpsize bar on\|off` | pasek u góry ekranu: HP i rozmiar moba, na którego patrzysz / którego bijesz |
| `/hpsize glowtiny on\|off`, `/hpsize glowtiny below <x>` | podświetla malutkie moby (w filmie ginęły z oczu) |
| `/hpsize info [cele]` | HP i rozmiar moba, na którego patrzysz |
| `/hpsize reset` | ustawienia domyślne |

Ustawienia zapisują się w folderze świata (`hpsize.json`). Rozmiar nie jest zapisywany w mobach,
więc po wyłączeniu lub odinstalowaniu moda świat wraca do normy. Ender Dragon i stojaki na zbroję
są domyślnie wykluczone (gra i tak nie pokazuje większego smoka).

## Komendy do ustawiania ujęć z mobami

| Komenda | Co robi |
|---|---|
| `/spawnsized <mob> <hp> [ilość]` | przywołuje moba z danym HP, czyli od razu w danym rozmiarze, np. `/spawnsized minecraft:chicken 16` |
| `/mobhp <cele> <hp>` | ustawia HP (a więc rozmiar), np. żeby zmniejszyć piglina do handlu bez bicia |
| `/mobhp <cele> max <hp>` | ustawia maks. HP i leczy do pełna |
| `/heal [cele]` | leczy graczy (też głód) lub moby — moby wracają do pełnego rozmiaru |
| `/glow <cele> [sekundy]` | podświetla moby (0 = wyłącz) |
| `/mute <cele>` / `/unmute <cele>` | wycisza moby (np. hałaśliwe pigliny przy handlu) |

## Komendy do nagrywania

| Komenda | Co robi |
|---|---|
| `/go [sekundy] [limit]` | start odcinka: zamraża graczy → odliczanie → START → odmraża → uruchamia timer (np. `/go 5 1h`) |
| `/countdown <sekundy> [tekst]` | odliczanie na środku ekranu z dźwiękiem; `/countdown cancel` |
| `/timer start` | stoper na pasku u góry (liczy czas gry — pauzuje się razem z grą) |
| `/timer countdown <czas>` | odliczanie, np. `10m`, `1h30m`, `90s`, `10:00` |
| `/timer pause` / `resume` / `stop` | pauza / wznowienie / ukrycie |
| `/timer add <czas>` / `remove <czas>` / `set <czas>` | zmiana czasu |
| `/timer label <tekst>` | napis przed czasem, np. `Dzień 1` (obsługuje kolory `&c`) |
| `/timer display bossbar\|actionbar` | gdzie pokazywać timer |
| `/freeze [gracze]` / `/unfreeze [gracze]` | zatrzymuje graczy w miejscu (mogą się rozglądać) |
| `/announce <tytuł>[\|podtytuł]` | duży napis na ekranie dla wszystkich, np. `/announce &6Rozdział 2\|Nether` |
| `/recmode on` / `off` | ukrywa na czacie komunikaty komend, potwierdzenia lecą na pasek akcji |
| `/cam` | tryb kamerzysty (widz); drugie `/cam` wraca na to samo miejsce i w ten sam tryb |
| `/nv [gracze]` | noktowizja bez cząsteczek (w filmie było za ciemno w Netherze) |
| `/cleanup [items\|mobs\|all] [promień]` | usuwa leżące przedmioty/strzały/XP lub wrogie moby — mniej lagów przy gigantach |

## Po stronie klienta (jeśli masz moda u siebie)

| Klawisz / komenda | Co robi |
|---|---|
| `H` | **czysty HUD**: chowa serca, pasek przedmiotów, celownik itp., ale zostawia rękę, czat, napisy i paski (inaczej niż F1) |
| `Z` (przytrzymaj) | **zoom** do filmowania gigantów z daleka, kółko myszy zmienia przybliżenie |
| `/hud on\|off`, `/hud hide <element>`, `/hud show <element>`, `/hud list` | wybór elementów ukrywanych przez czysty HUD |
| `/zoom <x>` | domyślne przybliżenie |

Klawisze zmienisz w Opcje → Sterowanie → HP Size.

## Budowanie

Gotowy plik `.jar` buduje GitHub Actions (zakładka **Actions** → workflow `hpsize` → artefakt `hpsize-mod`).
Lokalnie: JDK 25 i `./gradlew build` w folderze `hpsize` (plik w `build/libs/`).

Testy automatyczne (`src/gametest`) sprawdzają mechanikę i komendy na serwerze, a w CI uruchamiają
też prawdziwego klienta Minecrafta, który robi zrzuty ekranu (artefakt `client-test-screenshots`).
