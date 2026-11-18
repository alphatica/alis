# Stopa wolna od ryzyka a Sharpe, alfa Jensena i Treynor

Notatka techniczna: jak Alis liczy miary zależne od stopy wolnej od ryzyka (rf). Stan na 2026-11-17.

- rf i miary z rf działają w `alis` i `studio` (portfel i backtest).
- Alfa, Sharpe i Treynor są rozbite na składowe w Edge analysis (zob. [Rozbicie alfy](#rozbicie-alfy-edge-analysis), [Rozbicie Sharpe’a](#rozbicie-na-zwrot-i-zmienność-edge-analysis) i [Rozbicie Treynora](#rozbicie-treynora-edge-analysis)).
- Sharpe, Sortino, Calmar, zwrot / max downside DD i alfa Jensena są też celami optymalizacji i better exit (zob. [Cele optymalizacji](#cele-optymalizacji)).
- Plik `nbp_stopy_procentowe.csv` w tym katalogu to źródłowe stopy NBP (zob. [Źródło rf](#źródło-rf)).

## Przyjęte decyzje

- rf ≠ 0. Przy rf > 0 założenie rf = 0 zawyża Sharpe. To, czy gotówka w backteście przynosi odsetki, nie ma tu znaczenia: to osobna kwestia realizmu symulacji, która wpływa na `r_p`, a nie na rf.
- rf to stopa referencyjna NBP (uzasadnienie w [Źródło rf](#źródło-rf)).
- Beta to **beta całego konta** z regresji dziennych nadwyżkowych stóp zwrotu konta względem WIG. Bez bet poszczególnych spółek.
- Betę liczymy **w okresach stałej rf**: każda zmiana rf zaczyna nowy okres. Bety z jednej regresji na całym okresie (jak w Morningstarze) nie liczymy.
- Okres, w którym nie da się policzyć bety (mniej niż 2 sesje albo benchmark bez zmienności), jest dołączany do poprzedniego. Jeśli taki jest pierwszy okres, dołączamy go do następnego.
- Przed pierwszą datą w danych obowiązuje pierwsza stopa, po ostatniej ostatnia.
- `freeRate` zwraca ułamek (0.0375), a nie procent, żeby nie dzielić przy każdym użyciu.
- Błędny plik stóp przerywa program z komunikatem `plik:linia: powód`.
- Plik stóp powstaje dopiero po zaakceptowaniu warunków licencji.

## Implementacja

| Miejsce | Co robi |
|---|---|
| `alis/.../data/riskfreerate/Country` | enum, na razie tylko `PL`; nazwa pliku `pl.csv` |
| `alis/.../data/riskfreerate/RiskFreeRates` | `load(Path dir)`, `freeRate(Country, Time)`, `periods(Country, from, to)` |
| `alis/.../data/riskfreerate/RatePeriod` | `(from, to, rate)`: `from` i `to` włącznie, `rate` jako ułamek |
| `alis/.../data/riskfreerate/RateFile` | parsowanie, łączenie z danymi wbudowanymi, zapis (niepubliczna) |
| `alis/src/main/resources/.../riskfreerate/pl.csv` | dane wbudowane: stopa referencyjna NBP, 94 zmiany od 1998-02-26 do 2026-03-05 |
| `alis/.../trading/account/ReturnRatios` | `sharpe`, `sortino`, `versusBenchmark` (beta, alfa, Treynor), `calmar`; `benchmarkSessions`: nadwyżki, rf i okres bety każdej sesji, z których liczy się `versusBenchmark` |
| `alis/.../trading/account/SessionHistory` | `exposureAtStart()`: zainwestowana część konta na początku każdej sesji |
| `alis/.../trading/account/attribution/AlphaAttribution` | rozbicie alfy na dobór spółek, timing i gotówkę |
| `alis/.../trading/account/attribution/SharpeAttribution` | nadwyżka i zmienność konta i WIG-u, rozbicie różnicy Sharpe’a na efekt zwrotu i efekt zmienności |
| `alis/.../trading/account/attribution/TreynorAttribution` | rozbicie różnicy Treynora konta i WIG-u na efekt zwrotu i efekt bety |
| `alis/.../trading/account/BenchmarkReturns` | stopy zwrotu benchmarku w sesjach konta, dobierane po dacie sesji |
| `alis/.../data/time/Time` | dodane `Time.of(LocalDate)` i `epochDay()` |
| `studio-core/.../logic/riskfreerate/RiskFreeRatesProvider` | trzyma stopy z `~/Alphatica/rf` |
| `studio-core/.../logic/stats/RatioStats` | statystyki wspólne dla portfela i backtestu; na sztywno `COUNTRY = PL`, `BENCHMARK = wig` |
| `studio-core/.../logic/stats/edge/EdgeStats`, `EdgeCards` | karty Edge analysis, z rf z `RiskFreeRatesProvider` |
| `studio/.../web/WebStart`, `LicenseGate` | wczytanie przy starcie (gdy licencja jest zaakceptowana) albo przy akceptacji |

Testy: `RiskFreeRatesTest`, `ReturnRatiosTest`, `SessionHistoryTest`, `RatioScorersTest`, `BenchmarkReturnsTest`, `StudioOptionsTest`, `AlphaAttributionTest`, `SharpeAttributionTest`, `TreynorAttributionTest`.

### Plik `~/Alphatica/rf/pl.csv`

- Pierwsza linia to nagłówek `data_zmiany,stopa_procentowa`, potem wiersze `2026-03-05,3.75`: data ISO, stopa roczna w %, kropka dziesiętna.
- Daty muszą rosnąć, bo stopa obowiązuje od swojej daty do następnej zmiany. Wiersz z datą równą lub wcześniejszą od poprzedniej to błąd.
- Brak nagłówka i znak BOM na początku są akceptowane.
- Łączenie z danymi wbudowanymi (porównanie po dacie):

  | sytuacja | wynik |
  |---|---|
  | brak pliku | tworzony z danych wbudowanych |
  | data jest w pliku i w programie, stopy się różnią | obowiązuje plik |
  | data jest tylko w pliku | jest używana |
  | data jest tylko w programie | wstawiana do pliku według daty (plik jest przepisywany) |

- Wiersz wbudowany usunięty z pliku wraca przy następnym starcie. Żeby wyłączyć zmianę stopy, trzeba w tym wierszu wpisać poprzednią wartość.
- Kolejne wiersze z tą samą stopą nie tworzą nowego okresu.
- Innego kraju dotyczy inny plik w tym samym katalogu, np. `us.csv`, gdy dojdzie `Country.US`.

### Wzory w kodzie

- Stopa sesji `t`: `rf_t = rate · Δdni_kalendarzowych(t−1, t) / 365`, gdzie `rate` to stopa obowiązująca w dniu sesji `t−1` (na początku okresu stopy zwrotu). Pierwsza sesja liczy się jako 1 dzień ze stopą z jej dnia.
- rf jest liczone w locie, kursorem po okresach, bez tablicy rf dla każdej sesji.
- Sharpe i Sortino liczone na całej serii `e_t = r_t − rf_t`, annualizowane przez √252.
- Okres k (sesja należy do okresu, z którego pochodzi jej rf):
  - `β_k = cov(e_p, e_m) / var(e_m)`;
  - `α_k = mean(r_p − [rf + β_k·(r_m − rf)])`.
- Całość, gdzie `n_k` to liczba sesji w okresie k, a N suma wszystkich:
  - `β̄ = Σ (n_k/N)·β_k`;
  - `α = Σ (n_k/N)·α_k · 252` w % rocznie;
  - `T = mean(e_p) · 252 / β̄` w % rocznie.
- Sesje bez notowania benchmarku są pomijane w becie, alfie i Treynorze.
- Momenty liczone metodą Welforda. Łączenie okresów sumuje momenty dokładnie, tak jakby wszystkie sesje były w jednym okresie.
- Calmar: `CAGR / |max DD|`, bez rf.

### Studio

- **Portfel i backtest** pokazują: Total return, CAGR (252 sesje = rok), Sharpe, Sortino, Calmar, Return / max downside DD, Jensen's alpha, Beta i Treynor ratio (benchmark WIG, bez dopisku w nazwie).
- **Edge analysis** (portfel, backtest, walk-forward) ma karty Jensen's alpha (wykres pomostowy) i Sharpe & Treynor (tabela nadwyżki, zmienności, bety, Sharpe’a i Treynora konta i WIG-u oraz dwa wykresy pomostowe). Wartości są te same co w statystykach, bo liczą się z tych samych sesji, rf i bet okresów.
- **Backtest**: seria sesji pochodzi z konta (`AccountHistory.getSessions()`), pierwsza sesja liczy się od gotówki początkowej konta.
- **`RiskFreeRatesProvider.get()`** rzuca `IllegalStateException`, jeśli stopy nie zostały wczytane. Do API nie da się wejść przed akceptacją licencji, a wczytanie następuje przy starcie albo przy akceptacji.
- **Przykład** (portfel użytkownika, 747 sesji, 2023-10-30 – 2026-10-26):

  | statystyka | wartość |
  |---|---|
  | Sharpe | 1,32 |
  | Sortino | 1,95 |
  | alfa | 17,2 % |
  | beta | 0,50 |
  | Treynor | 60,3 % |
  | średnia rf | 5,00 % |

## Cele optymalizacji

- **Seria sesji w koncie:** `AccountHistory.getSessions()` (`SessionHistory`) zapisuje NAV na koniec każdej sesji w `Account.updateLastKnown` i wpłaty netto sesji. Delisting i `close()` po wycenie nadpisują NAV ostatniej sesji, jak w backteście studia. Interfejs `AccountScorer` się nie zmienił.
- **Stopy zwrotu ważone czasem:** `NAV / (poprzedni NAV + wpłaty netto sesji) − 1`. Przepływ liczy się na początek następnej wycenianej sesji, a przepływy po ostatniej wycenie (np. wypłata w `SimulatedAccount.finish()`) są pomijane. Ma to znaczenie w better exit, który dokłada sztuczne wpłaty i wypłaty.
- **Scorery** (`alis/.../scorer/`): `SharpeRatio(rates, country)`, `SortinoRatio(rates, country)`, `JensensAlpha(rates, country, benchmark)`, `CalmarRatio`, `ReturnToMaxDownsideDD`. `periods()` jest wywoływane raz na ocenę, czyli raz na przebieg.
- **Alfa Jensena:** `JensensAlpha` dostaje cały `Market` WIG-u i dobiera jego stopy zwrotu po datach sesji konta, więc inny start („Fuzzy start time”) nie wymaga zmian. Wynik w % rocznie.
- **Wynik zawsze skończony:** NaN i ∞ zamieniane na 0 (`Scores.finiteOrZero`), bo `GeneticParamsSelector` używa wyników jako wag, a NaN w `Double.compare` byłby najlepszy.
- **Zwrot / max downside DD:** całkowity zwrot ważony czasem w % podzielony przez |max downside DD| w % (`ReturnRatios.returnToMaxDrawdown`). Bez obsunięcia: scorer 0, statystyka „unknown”. Pokazywany w portfelu, backteście i jako kolumna wyników optymalizacji.
- **Jedno źródło serii:** backtest i portfel w studiu (statystyki, wykres equity, analiza obsunięć) też biorą serię z `AccountHistory.getSessions()`, więc wynik scorera i statystyka liczą się z tych samych danych. `TimeWeightedReturn` i `ReturnRatios.sessionReturns` usunięte.
- **Studio:** wpisy w `StudioOptions.EXIT_SCORERS` (czyli też `SCORERS`) są typu `ScorerOption`, który zadanie rozwiązuje raz, na danych, na których działa (`Inputs.scorer`). Alfa bierze wtedy WIG z tych danych, więc wszystkie przebiegi zadania liczą się z tym samym WIG-iem, nawet gdy w trakcie wczytane zostaną inne dane. Bez WIG-u w danych żądanie jest odrzucane z komunikatem. rf z `RiskFreeRatesProvider`, kraj z `RatioStats.COUNTRY`. Wynik w tabelach optymalizacji i better exit ma do 2 miejsc po przecinku (`Column.score`).

## Następne kroki

1. **Kraj i benchmark według danych.**
   - Teraz `RatioStats` ma na sztywno PL i WIG. Backtest na danych US (`load-us`) liczy Sharpe'a i Sortino z polską stopą, a beta, alfa i Treynor wychodzą „unknown”, bo w danych US nie ma WIG-u.
   - Do zrobienia: `Country.US` z wbudowanymi danymi (np. 3-miesięczne T-bille) i wybór kraju oraz benchmarku według załadowanych danych.
2. **Do rozważenia (nieuzgodnione):** Sharpe z danych miesięcznych, porównywalny z Morningstarem.

## Źródło rf

`nbp_stopy_procentowe.csv`: stopa referencyjna, depozytowa i lombardowa NBP, 96 zmian od 1998-02-26 do 2026-03-05. Do danych wbudowanych trafiła tylko kolumna `referencyjna`, bez dwóch wierszy (2001-12-01 i 2010-01-01), w których zmieniły się tylko inne stopy.
- Źródło: `https://static.nbp.pl/dane/stopy/stopy_procentowe_archiwum.xml`. Atrybut `data_publikacji="2015-03-04"` w pliku jest nieaktualny, bo plik zawiera zmiany do 2026. Aktualne stopy są w `https://static.nbp.pl/dane/stopy/stopy_procentowe.xml`.
- `obowiazuje_od` to data wejścia stopy w życie.
- Brak danych sprzed 1998-02-26. W programie przed tą datą obowiązuje pierwsza stopa (24%), a użytkownik może dopisać starsze stopy do pliku.

Średnia roczna stopa referencyjna, % (ważona dniami kalendarzowymi; 2026 do 27.10):

| rok | ref | rok | ref | rok | ref | rok | ref |
|---|---|---|---|---|---|---|---|
| 1999 | 13,71 | 2006 | 4,06 | 2013 | 2,94 | 2020 | 0,50 |
| 2000 | 17,86 | 2007 | 4,41 | 2014 | 2,38 | 2021 | 0,34 |
| 2001 | 15,86 | 2008 | 5,72 | 2015 | 1,59 | 2022 | 5,28 |
| 2002 | 8,81 | 2009 | 3,77 | 2016 | 1,50 | 2023 | 6,45 |
| 2003 | 5,67 | 2010 | 3,50 | 2017 | 1,50 | 2024 | 5,75 |
| 2004 | 5,78 | 2011 | 4,22 | 2018 | 1,50 | 2025 | 5,10 |
| 2005 | 5,34 | 2012 | 4,61 | 2019 | 1,50 | 2026 | 3,80 |

### Dlaczego referencyjna

- **Ryzyko emitenta jest najbliższe zeru.** Stopa referencyjna to rentowność 7-dniowych bonów pieniężnych NBP (opis w pliku NBP), a emitentem jest bank centralny. WIBOR to stopa, po jakiej banki pożyczają sobie nawzajem, więc zawiera ryzyko kredytowe banków.
- **Okres zapadalności pasuje do dziennych stóp zwrotu.** Przy dziennych `r_t − rf_t` 7 dni pasuje lepiej niż 3 miesiące. WIBOR 3M zawiera dodatkowo oczekiwania co do zmian stóp: w 2022 r. wyprzedzał podwyżki i był wyraźnie wyżej.
- **Pełna historia od 1998 r.** jest w oficjalnym źródle, bez luk.
- **Polskich bonów skarbowych nie da się użyć.** To byłby odpowiednik amerykańskiego T-billa (standard Morningstara), ale emisje Ministerstwa Finansów były nieregularne, z przerwami trwającymi lata, więc nie ma ciągłej serii.
- **Lombardowa nie pasuje:** to górna granica korytarza NBP, po tej stopie banki pożyczają od NBP pod zastaw.
- **Depozytowa nie pasuje:** to dolna granica, overnight dla banków. Zwykle 0,5 pp poniżej referencyjnej, a w 2020 r. przez ponad rok wynosiła 0%, więc zaniżałaby rf.

Wpływ różnicy między stopą referencyjną a WIBOR-em 3M, przyjmując 0,25 pp. Różnica to zwykle ułamek punktu procentowego, w 2022 r. więcej; wartości z pamięci, niesprawdzone.

| miara | wpływ |
|---|---|
| Sharpe (σ = 20%) | ~0,01 |
| alfa Jensena | `(1−β)·0,25 pp` rocznie, np. 0,125 pp przy β = 0,5 |
| Treynor | `0,25 pp / β̄` |

To mało w porównaniu z innymi źródłami błędu: częstotliwością danych, WIG zamiast WIG20 i błędem β w krótkich podokresach. Jeśli potrzebna będzie stopa rynkowa: następcą WIBOR-u jest WIRON (overnight). Nie sprawdzano, czy jego historia jest dostępna za darmo.

Uwagi o źródłach:
- Stopa referencyjna to stopa polityki NBP, nie stopa rynkowa. WIBOR 3M zwykle jest nieco wyżej, w 2022 r. różnica była wyraźnie większa. Średnie WIBOR podawane w sesji (np. „2022 ~6%”) były z pamięci i nie zostały sprawdzone.
- Stooq: bezpośrednie pobieranie CSV (`stooq.pl/q/d/l/?s=...`) jest zablokowane wyzwaniem JavaScript. Symbole WIBOR (`plopln3m` itd.) i rentowności (`2ply.b`, `10ply.b`) nie zostały zweryfikowane.
- Lokalna paczka `d_pl_txt.zip` (`~/Alphatica/stooq_gpw/stooq_data/data/daily/pl`) nie zawiera WIBOR-u. Zamienniki z tej paczki sprawdzone i odrzucone:
  - „funds money market” (163 pliki): w 2021–22 przeważnie stopy ujemne, w 2023 +8–12%. Mają ryzyko stopy procentowej i rok do roku nie śledzą rf.
  - `etfbtbsp`: indeks obligacji TBSP, czyli długie obligacje (2022 −5,6%, 2023 +11,7%).
  - `etfbcash`: do 2023 r. nie zachowuje się jak fundusz gotówkowy (2022 −22%), od 2024 r. wiarygodny.
  - `etfpzucash`: notowany dopiero od 08.2026.

## Sharpe

- Definicja (Sharpe 1994, Morningstar): `e_t = r_t − rf_t`, `SR = mean(e)/std(e) · √N` (N = 252 albo 12).
- Uproszczenie: `mean(r − rf) = mean(r) − mean(rf)`, więc licznik wychodzi dokładnie ten sam przy średniej rf z **dokładnie tego samego okresu**. Mianownik `std(r − rf)` vs `std(r)` różni się o ~0,1% (wariancja rf jest pomijalna przy wariancji akcji).
- Błąd w rf o 1 pp przy σ = 20% zmienia Sharpe o ~0,05.
- **Nie uśredniać Sharpe’ów podokresów**, także ważonych czasem. To iloraz, a średnia ilorazów pomija wariancję średnich między podokresami, więc jest zawyżona. Przykład: A (mean 1%, std 2%, SR 0,5) i B (0%, 10%, 0) dają średnią 0,25, a Sharpe całego okresu wynosi 0,07.
- Błąd: `(CAGR − rf)/σ` łączy średnią geometryczną z arytmetycznym σ, przez co licznik różni się o ~σ²/2 (~2 pp przy σ = 20%).
- Częstotliwość ma większe znaczenie niż rf: annualizacja przez √N zakłada niezależne stopy zwrotu, a przy autokorelacji wynik z danych dziennych i miesięcznych potrafi różnić się o kilkadziesiąt procent (Lo 2002). Dane miesięczne są porównywalne z Morningstarem.
- Błąd standardowy Sharpe’a ≈ `√((1 + SR²/2)/n)` na okres.
- Sortino: ta sama seria `e_t`, w mianowniku odchylenie poniżej 0.

### Rozbicie na zwrot i zmienność (Edge analysis)

Pokazuje, które składowe dają Sharpe’a konta wyższego (albo niższego) niż Sharpe WIG-u. Część karty „Sharpe & Treynor”, razem z [rozbiciem Treynora](#rozbicie-treynora-edge-analysis).

- Oznaczenia: `μ = mean(e)`, `σ = std(e)` (z próby) jak w `ReturnRatios.sharpe`; `a` to konto, `m` to WIG. Obie serie na sesjach z `benchmarkSessions` (te same co przy becie) i z tą samą rf: sesje bez notowania WIG-u pomijane po obu stronach, więc przy takich sesjach Sharpe konta może się minimalnie różnić od statystyk.
- Rozbicie symetryczne (Shapley), sumuje się dokładnie do `SR_a − SR_m`:
  - efekt zwrotu: `(μ_a − μ_m) · ½(1/σ_a + 1/σ_m) · √252`;
  - efekt zmienności: `½(μ_a + μ_m) · (1/σ_a − 1/σ_m) · √252`.
- Efekt zwrotu w Sharpe’ie i w Treynorze to różne liczby (różne mianowniki), więc karta ma dwa wykresy pomostowe.
- Odrzucone warianty:
  - sekwencyjny (najpierw zwrot, potem σ) daje inne liczby przy odwrotnej kolejności;
  - `ln(SR_a/SR_m) = ln(μ_a/μ_m) + ln(σ_m/σ_a)` nie działa przy `μ ≤ 0`, a nadwyżka WIG-u nad rf bywa ujemna (np. 2007–2013).
- Przy `½(μ_a + μ_m) < 0` mniejsze odchylenie obniża Sharpe’a. To właściwość samego Sharpe’a (ujemny Sharpe „rośnie” z odchyleniem); mówi o tym pomoc karty.
- Widok: karta „Sharpe & Treynor”:
  - tabela konta i WIG-u: nadwyżka rocznie (`μ · 252`), zmienność rocznie (`σ · √252`), beta, Sharpe, Treynor;
  - wykres pomostowy Sharpe’a: Sharpe WIG-u → + efekt zwrotu → + efekt zmienności → = Sharpe konta;
  - wykres pomostowy Treynora (opis niżej). Bez wykresów w czasie.
- Gdy beta pomaga Treynorowi, a zmienność szkodzi Sharpe’owi, duża część ryzyka konta nie idzie z WIG-iem: Treynor jej nie widzi, Sharpe tak.
- Wyniki:

  | | nadwyżka / rok | zmienność / rok | beta | Sharpe: WIG → zwrot → zmienność → konto | Treynor konta / WIG-u |
  |---|---|---|---|---|---|
  | SmaCross 2015–2026 | 25,4% (WIG 8,4%) | 29,0% (WIG 18,6%) | 0,62 | 0,45 → +0,75 → −0,33 → 0,87 | 40,6% / 8,4% |
  | portfel, 2026-11-17 | 29,3% (WIG 24,2%) | 22,5% (WIG 17,8%) | 0,49 | 1,36 → +0,26 → −0,31 → 1,30 | 59,5% / 24,2% |

  Portfel ma Treynora 2,5 raza wyższego niż WIG, a Sharpe’a niższego, bo przy becie 0,49 jego zmienność jest większa niż WIG-u.
- Nieużywane (gdyby potrzebny był wykres w czasie): wkład sesji t (`x = e_a`, `y = e_m`, N sesji), skumulowane kończą się dokładnie na obu efektach:
  - zwrot: `(x_t − y_t)/N · ½(1/σ_a + 1/σ_m) · √252`;
  - zmienność: `½(μ_a + μ_m) · √252 · [(y_t − μ_m)² − (x_t − μ_a)²] / ((N−1) · σ_a · σ_m · (σ_a + σ_m))`, z tożsamości `1/σ_a − 1/σ_m = (σ_m² − σ_a²) / (σ_a · σ_m · (σ_a + σ_m))` i wariancji jako sumy kwadratów odchyleń.
- Możliwe rozszerzenie: efekt zmienności rozbity na część z gotówki (ekspozycja) i część z mniej zmiennych spółek.

## Alfa Jensena

- `r_p − [rf + β(r_m − rf)]` = `e_p − β·e_m`. Średnia tego wyrażenia równa się wyrazowi wolnemu regresji OLS `e_p = α + β·e_m + ε`, jeśli β pochodzi z tej samej regresji na tych samych danych. Zmienne rf jest tu standardowym przypadkiem (Jensen 1968).
- Ze średnim rf: `α = mean(r_p) − β·mean(r_m) − (1−β)·mean(rf)`. Błąd w rf o 1 pp daje błąd alfy `(1−β)` pp. Przy strategiach często będących w gotówce (niskie β) alfa jest bardziej wrażliwa na rf niż Sharpe.
- Błąd: regresja surowych stóp zwrotu bez rf. Wyraz wolny wynosi wtedy `α + (1−β)·mean(rf)`, czyli przy β = 0,3 i rf = 6% jest zawyżony o ~4 pp rocznie (test `shouldLowerAlphaByOneMinusBetaTimesRiskFreeRate`).
- Benchmark: **WIG** (indeks dochodowy, z dywidendami), nie WIG20 (indeks cenowy). Porównanie z WIG20 zawyża alfę o ~(2–4 pp)·β rocznie.
- Istotność: przy 10 latach danych miesięcznych i σ(ε) = 4 pp/mies. błąd α wynosi ~4,4 pp rocznie. Przy autokorelacji reszt użyć błędu Neweya–Westa.

## Beta konta zmienna w czasie

Przyjęte podejście: β_k z regresji dziennych `e_p` względem `e_m` w okresach stałej rf.

- **Alfa jest addytywna:** `α = Σ_k (n_k/N)·α_k`. Odpowiada to modelowi z betą stałą w każdym okresie.
- **Treynor liczyć raz, nie uśredniać:** `T = mean(e_p)/β̄`. W okresie z β_k ≈ 0 (głównie gotówka) Treynor okresu dąży do ±∞. Przy małym β̄ Treynor jest niestabilny również w tej postaci.
- **Długość okresów:** okresy ze stałą stopą bywają bardzo nierówne. 03.2015–03.2020 to ~1250 sesji, a w 2022 r. bywało ~20 sesji. Błąd β_k ≈ `σ_ε/(σ_m·√n)`, przy σ_ε ≈ σ_m:

  | sesje | błąd β_k |
  |---|---|
  | 20 | ~0,22 |
  | 100 | ~0,10 |
  | 250 | ~0,06 |

  Przy wagach `n_k/N` błąd β̄ wynosi `σ_ε/(σ_m·√N)`, tyle samo co przy jednej regresji. Niepewne są tylko bety pojedynczych krótkich okresów, a tych nie pokazujemy.
- Okresy według zmian rf nie pokrywają się ze zmianami ekspozycji strategii. To świadoma decyzja, alternatywą był podział kalendarzowy.
- Raporty dla inwestorów (Morningstar) podają betę z jednej regresji na całym okresie albo w oknie 36-miesięcznym. Przez to nasze wartości nie są z nimi wprost porównywalne.
- OLS ze stałym β na okresie, w którym ekspozycja się zmienia, nie daje średniej bety, tylko średnią β_t ważoną `(e_m,t − mean e_m)²`, czyli z większą wagą okresów dużych ruchów rynku.

### Rozbicie alfy (Edge analysis)

Timing jest częścią alfy: alfa to całkowita wartość dodana ponad średnią betę. Karta w Edge analysis rozdziela ją na trzy części, bez bet spółek.

- Oznaczenia sesji t w okresie bety k: `e_a`, `e_m` nadwyżki konta i WIG-u, `rf` stopa wolna od ryzyka sesji, `x_t` zainwestowana część konta na początku sesji (`SessionHistory.exposureAtStart()`: pozycje z poprzedniej wyceny przez poprzedni NAV plus wpłaty netto sesji), β_k beta okresu jak w `versusBenchmark`.
- Gotówka w kontach Alis nie przynosi odsetek, więc jej część nadwyżki to `cash_t = −(1 − x_t)·rf_t`. Reszta: `y_t = e_a,t − cash_t`.
- `b_k`: nachylenie regresji OLS `y` względem `z = x·e_m` w okresie, czyli beta wybieranych spółek na jednostkę zainwestowanej części. Gdy `z` się nie zmienia (np. cały okres w gotówce), `b_k = 0`.
- Wkład sesji (rocznie: `· 252 · 100 / N`, N to sesje z okresem bety):
  - **dobór spółek:** `y_t − b_k·x_t·e_m,t`: o ile spółki przebiły WIG przy części zainwestowanej tej sesji;
  - **timing:** `b_k·x_t·e_m,t − β_k·e_m,t`: zainwestowana część tej sesji zamiast bety okresu; ogólnie ≈ `b·cov(x_t, e_m,t)`, czyli dodatni, gdy konto jest zainwestowane w dni wzrostów i w gotówce w dni spadków;
  - **gotówka:** `cash_t`.
- W każdej sesji trzy wkłady sumują się do `e_a,t − β_k·e_m,t`, więc ich suma to dokładnie alfa z `versusBenchmark` (`α = Σ_k (n_k/N)·α_k`).
- Wykres pomostowy: Index × beta (`mean(β_k·e_m)`) → + dobór → + timing → + gotówka → = nadwyżka konta, pod nim alfa i beta. Bez wykresu w czasie.
- Przykłady (dwie sesje, beta spółek 1):
  - konto w 100% w dniu wzrostu WIG-u o 2% i w gotówce w dniu spadku o 2%: β ≈ 0,5, alfa = 1 pp dziennie, cała z timingu;
  - konto zawsze w 100%, spółki o 1 pp lepsze od WIG-u: β = 1, alfa = 1 pp dziennie, cała z doboru.
- Ograniczenia:
  - `x_t` jest z poprzedniej wyceny, a transakcje są na otwarciu: ruch spółki kupionej na otwarciu w dniu kupna (otwarcie → zamknięcie) trafia do doboru, a w dniu sprzedaży WIG liczy się dla całej sesji;
  - model zna tylko ilość gotówki: zmiana bety samych spółek w czasie (np. bardziej ryzykowne spółki w hossie) trafia do doboru;
  - Index × beta to średnia `β_k·e_m` po okresach, więc może mieć inny znak niż nadwyżka WIG-u (SmaCross 2007–2013: +1,6% przy nadwyżce WIG-u −2,6% rocznie).
- Wyniki (SmaCross, parametry domyślne, GPW, prowizja 0,3%), % rocznie:

  | okres | alfa | dobór | timing | gotówka | beta |
  |---|---|---|---|---|---|
  | 2015–2026 | 20,1 | 20,1 | 0,1 | −0,1 | 0,62 |
  | 2007–2013 | −4,9 | −2,3 | −1,9 | −0,8 | 0,53 |

- Klasyczne modele timingu (nieużywane): Treynor–Mazuy (`+γ·e_m²`) i Henriksson–Merton (`+γ·max(0, e_m)`).

### Rozbicie Treynora (Edge analysis)

- Treynor WIG-u to jego średnia nadwyżka `μ_m` (beta 1). Różnica `T_a − T_m` jest rozbita symetrycznie (Shapley), tak jak Sharpe, z betą zamiast odchylenia:
  - efekt zwrotu: `(μ_a − μ_m) · ½(1/β̄ + 1)`;
  - efekt bety: `½(μ_a + μ_m) · (1/β̄ − 1)`;
  - w % rocznie (`· 252 · 100`); suma jest dokładnie `T_a − T_m`, a `T_a` to Treynor ze statystyk.
- Wykres pomostowy w karcie „Sharpe & Treynor”: Treynor WIG-u → + efekt zwrotu → + efekt bety → = Treynor konta. Wykresu w czasie nie da się zrobić, bo β̄ to średnia ilorazów `cov_k/var_k` i nie rozkłada się na wkłady sesji.
- `T_a − T_m = (μ_a − β̄·μ_m)/β̄`. Przy jednym okresie bety to alfa / beta, więc przy niskiej becie (dużo gotówki) nawet mała alfa daje wysoki Treynor. Przy betach liczonych w okresach rf może się to bardzo różnić od alfy / bety (SmaCross 2007–2013: −3,8 pp przy alfie / becie −9,4), dlatego karta tego nie pokazuje.
- Przy `½(μ_a + μ_m) < 0` niższa beta obniża Treynora.
- Przykład (portfel użytkownika, 2026-11-17), % rocznie: WIG 24,2 → efekt zwrotu +7,8 → efekt bety +27,6 → konto 59,5; beta 0,49. Przewaga nad WIG-iem pochodzi głównie z niskiej bety.

## Jak liczą inne programy (z pamięci, kodu nie sprawdzano)

| Program | rf | Częstotliwość |
|---|---|---|
| Morningstar | seria 3-mies. T-bill, osobno dla każdego miesiąca | miesięczna, √12 |
| Portfolio Visualizer | seria 1-mies. T-bill | miesięczna |
| QuantConnect LEAN | zmienna (historia stóp Fed) | dzienna |
| quantstats | domyślnie 0, stała lub seria; `greeks()` bez rf | dzienna, √252 |
| empyrical / pyfolio | domyślnie 0, stała za okres | dzienna, √252 |
| backtrader (SharpeRatio) | stała roczna, domyślnie 1% | domyślnie roczna |
| AmiBroker | stała z ustawień raportu | miesięczna |
