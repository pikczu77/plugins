# Body Upgrades — każde osiągnięcie ulepsza twoje ciało

Mod Fabric na **Minecraft 1.21.11** zrobiony na podstawie filmu
[„Minecraft but you can UPGRADE Yourself Infinitely”](https://youtu.be/XcSAHT6EgD8) (Craftee).
Za każde osiągnięcie z filmu dostajesz nową moc, a z ciała wyrasta nowa część: kilofy z ramienia, motyka na głowie,
ramię golema przez klatkę piersiową, łóżko na plecach, skrzydła smoka... Kod, modele i tekstury są autorskie
(tekstury generuje skrypt `tools/make_textures.py`, modele `tools/make_models.py`).

| Początek | Połowa filmu | Wszystko (przód) | Wszystko (tył) |
|---|---|---|---|
| ![](docs/start.png) | ![](docs/middle-front.png) | ![](docs/all-front.png) | ![](docs/all-back.png) |

## Instalacja

1. Zainstaluj [Fabric Loader](https://fabricmc.net/use/installer/) dla 1.21.11 (0.17.3 lub nowszy).
2. Wrzuć do folderu `mods`:
   - `upgrades-1.21.11-1.0.0.jar` (ten mod),
   - [Fabric API](https://modrinth.com/mod/fabric-api) dla 1.21.11.
3. Graj normalnie — ulepszenia przychodzą same z osiągnięciami.

Mod musi być **na serwerze i u graczy** (klient rysuje części ciała, klony i obsługuje moce pustą ręką).

## Ulepszenia (w kolejności z filmu)

| Osiągnięcie | Ulepszenie | Co robi | Część ciała |
|---|---|---|---|
| Epoka kamienia | **Pięści-Kilof** | pięść kopie jak drewniany kilof | drewniany kilof z ramienia |
| Lepsze narzędzia | **Vein Miner** | kopiesz całą grupę połączonych takich samych bloków (16); pięść = kamienny kilof | kamienny kilof doczepiony do drewnianego |
| Nasionka | **Zielona Rączka** | PPM pustą ręką na ziemię: zaorywuje 3×3 i od razu sadzi; uprawy wokół ciebie rosną błyskawicznie | motyka na głowie |
| Zdobycz sprzętowa | **Ramię Golema** | +3 obrażeń wręcz, ciosy podrzucają moby | ramię golema przez klatkę |
| Łowca potworów | **Miecz w Bucie** | ciosy wręcz ranią wszystko dookoła (także twoje klony — jak w filmie), sprint w moby rani i odrzuca | miecz z buta |
| Ubierz się | **Klon Bojowy Lv. 1** | gdy potwór cię zrani, pojawia się klon, który odciąga wrogów | żelazna figurka na ramieniu golema |
| Słodkich snów | **Łóżko na Plecach** | kucnij + PPM pustą ręką: rozkładasz duże łóżko i ucinasz drzemkę (o każdej porze); pełna drzemka leczy do pełna | łóżko na plecach |
| Ale okazja! | **Nos Handlarza** | kucnij + PPM pustą ręką: wywąchujesz moba albo blok, który chce handlować (świeci); PPM na nim otwiera oferty. Cena to rzeczy, które masz przy sobie, oferty wygasają po 30 s | nos wieśniaka |
| Cel! | **Spust** | PPM pustą ręką strzela strzałą | łuk na czole |
| Nie dziś, dziękuję | **Mini Tarcze** | pociski są blokowane i odbijane prosto w strzelca | tarcze na barkach |
| Czy to nie żelazny kilof? | **Vein Miner+** | 48 bloków, pięść = żelazny kilof | żelazny kilof w łańcuchu |
| Diamenty! | **Vein Miner++** | 128 bloków, pięść = diamentowy kilof | diamentowy kilof w łańcuchu |
| Okryj mnie diamentami | **Klon Bojowy Lv. 2** | klony walczą za ciebie, mają mało HP i po śmierci dzielą się na dwa mniejsze | diamentowa figurka |
| Gorący towar | **Gorące Łapy** | cios wręcz stawia pod ofiarą lawę, która znika po 4 s | wiadro lawy w pięści golema |
| Wyzwanie z wiadrem lodu | **Obsydianowy Róg** | PPM obsydianem rzuca nim: wybuch + obsydian w kraterze. Kucnij + PPM stawia normalnie | obsydianowy róg |
| Zaklinacz | **Zaklęte Ciało** | dropy z bloków i mobów ×2–×5 (z Vein Minerem = góry przedmiotów) | wszystkie części błyszczą |
| Musimy zejść głębiej | **Portal w Kieszeni** | PPM pustą ręką stawia portal tam, gdzie patrzysz (do 64 bloków); max 2, wejście w jeden wyrzuca z drugiego (też między wymiarami) | portal w kieszeni |
| W ogień | **Moc Blaze'a** | PPM różdżką Blaze'a: salwa 3 kul ognia (różdżka się nie zużywa) | krążące różdżki Blaze'a |
| Ukryte w głębinach | **Vein Miner MAX** | 512 bloków, pięść = netherytowy kilof | netherytowy kilof na końcu łańcucha |
| Okryj mnie gruzem | **Klon Bojowy Lv. 3** | silne klony z netherytowym mieczem, nieśmiertelne | netherytowa figurka |
| Szpiegowskie oko | **Oko Kresu** | przytrzymaj kucanie (stojąc) 2 s: losowy teleport, 35% szans na salę z portalem do Kresu | oko Kresu zamiast oka |
| Koniec? | **Skrzydła Smoka** | latasz; PPM pustą ręką strzela samonaprowadzającymi kulami ognia, które niszczą kryształy Kresu | skrzydła smoka |
| Następne pokolenie | **Następne Pokolenie** | małe klony, które same się mnożą... | smocze jajo na ramieniu |

Po odblokowaniu: napis **NOWE ULEPSZENIE!** z nazwą, dźwięk i opis mocy na czacie (pod komunikatem o osiągnięciu).
Ulepszenia wynikają z osiągnięć, więc działa też `/advancement grant|revoke` i nic nie jest zapisywane w świecie poza ustawieniami.

## Sterowanie

- **PPM pustą ręką** — moc „kliknięcia” (Zielona Rączka / Spust / Portal / Skrzydła Smoka).
- **Kucnij + PPM pustą ręką** — moc „kucania” (Drzemka / Nos Handlarza).
- **V** — zmienia moc pod PPM, **kucnij + V** — pod kucnij + PPM. Nowa moc wybiera się sama po odblokowaniu
  (jak w filmie: zawsze używasz najnowszej). Z pustą ręką nad paskiem szybkiego wyboru widać, co jest wybrane.
- **Kucanie przy kopaniu** — kopie pojedynczy blok (bez Vein Minera).

## Komendy (`/upgrades`)

| Komenda | Co robi |
|---|---|
| `/upgrades` / `/upgrades list [gracz]` | lista ulepszeń gracza |
| `/upgrades give <gracze> <ulepszenie\|all>` | nadaje ulepszenie (zalicza jego osiągnięcie) |
| `/upgrades take <gracze> <ulepszenie\|all>` | zabiera (cofa osiągnięcie) |
| `/upgrades on` / `off` | włącza / wyłącza wszystkie ulepszenia |
| `/upgrades enable\|disable <ulepszenie\|all>` | włącza / wyłącza pojedyncze ulepszenie |
| `/upgrades powers select` | jedna moc na przycisk, V zmienia (domyślne) |
| `/upgrades powers all` | wszystkie moce na przycisku odpalają naraz |
| `/upgrades titles true\|false` | napis NOWE ULEPSZENIE |
| `/upgrades veinminer sneak true\|false` | czy kucanie kopie pojedynczy blok |
| `/upgrades veinminer sizes <lv1> <lv2> <lv3> <max>` | ile bloków kopie Vein Miner (domyślnie 16 48 128 512) |
| `/upgrades drops <min> <max>` | mnożnik dropów Zaklętego Ciała (domyślnie 2–5) |
| `/upgrades clones max <n>` | maks. klonów bojowych na gracza (domyślnie 40) |
| `/upgrades clones multiplicity <n>` | maks. klonów z Następnego Pokolenia (domyślnie 150) |
| `/upgrades clones clear [gracz]` | usuwa klony |
| `/upgrades portals clear` | usuwa portale |
| `/upgrades settings` / `reset` | pokazuje / przywraca ustawienia |

Ustawienia zapisują się w folderze świata (`upgrades.json`).

## Co zmieniłem względem filmu (i dlaczego)

- **Jedna moc na przycisk + klawisz V.** W filmie Portal, Spust i Skrzydła są na tym samym PPM pustą ręką, a Drzemka
  i Nos na kucnij + PPM. Gdyby wszystko odpalało naraz, każde wywąchanie handlu kładłoby cię spać, a każdy strzał
  przestawiałby portal. Domyślnie działa najnowsza moc (tak to wyglądało w filmie), a „wszystko naraz” jest pod
  `/upgrades powers all`.
- **Limit klonów.** W filmie Następne Pokolenie mnożyło się bez końca i „zepsuło świat” — tu jest limit (150,
  do zmiany komendą), a `/upgrades clones clear` sprząta.
- **Kucanie = pojedynczy blok** przy kopaniu i **teleport Oka Kresu tylko na stojąco** — żeby nie rozkopać bazy ani
  nie teleportować się przy budowaniu na kucaka. Oba da się wyłączyć (`/upgrades veinminer sneak false`).
- **Obsydian stawiasz na kucaka** — w filmie po ulepszeniu nie dało się go postawić.
- Klony mają **twoją skórkę** (skórkę gracza, który je przywołał).

## Propozycje na później

- Osiągnięcia bez ulepszenia w filmie (np. „Straszliwa forteca”) mogłyby dawać małe bonusy (+serce, +szybkość) — tytuł
  obiecuje „nieskończone” ulepszenia.
- Portale zapisywane w świecie (teraz znikają po restarcie serwera).
- Ekran z drzewkiem ulepszeń pod klawiszem.

## Budowanie

```
./gradlew build                 # mod + testy serwerowe
./gradlew runClientGameTest     # prawdziwy klient, zrzuty ekranu w build/run/clientGameTest/screenshots
```
