# Checking simple trading strategies

This module contains various tests for trading strategies.

[Golden cross](./src/main/java/com/alphatica/alis/examples/goldencross)

Checking trading results when using golden cross (sma 50 / 200) as entry and exit signals.

[Simple moving average](./src/main/java/com/alphatica/alis/examples/sma)

Test for simple moving average of various lengths.

[Min-Max indicator](./src/main/java/com/alphatica/alis/examples/minmax)

A simple trend following indicator. Good replacement for SMA.

[Min-Max market breadth WIG signals](./src/main/java/com/alphatica/alis/examples/marketbreadth)

Using market breadth as a signal to "trade" WIG. As it turns out, market breadth is quite a powerful signal
and can give good trading results.

[All-time high + time stop](./src/main/java/com/alphatica/alis/examples/ath/AthPlusTime.java)

Very simple strategy: buy a stock after an all-time-high and just hold for 2 months.
This alone was enough to make almost 30% a year (CAGR) for the last 20 years.

[All-time high + time stop + pyramid + liquidity check](./src/main/java/com/alphatica/alis/examples/ath/AthPlusTimePyramid.java)

Similar strategy to the one above but with adding to a winning positions. Also, takes liquidity into account.

[Donchian channel breakout](./src/main/java/com/alphatica/alis/examples/donchian)

Donchian channel breakout strategy with/without optimization.

[Williams R indicator](./src/main/java/com/alphatica/alis/examples/williamsr)

William's R indicator tests (with optimization)