# Strategy tests

Backtests of simple trading strategies on historical data.

| Example | What it tests |
|---|---|
| [Golden cross](./src/main/java/com/alphatica/alis/examples/goldencross) | Buy when SMA 50 crosses above SMA 200, sell on the opposite cross. |
| [Simple moving average](./src/main/java/com/alphatica/alis/examples/sma) | SMA strategies with many SMA lengths. |
| [Min-Max indicator](./src/main/java/com/alphatica/alis/examples/minmax) | A simple trend indicator. Fewer false signals than an SMA. |
| [Min-Max market breadth on WIG](./src/main/java/com/alphatica/alis/examples/marketbreadth) | Trades the WIG index using market breadth. Breadth turns out to be a strong signal. |
| [All-time high + time stop](./src/main/java/com/alphatica/alis/examples/ath/AthPlusTime.java) | Buy a stock at an all-time high and hold it for 2 months. Close to 30% CAGR over the last 20 years. |
| [All-time high + time stop + pyramid](./src/main/java/com/alphatica/alis/examples/ath/AthPlusTimePyramid.java) | As above, plus adding to winning positions and a liquidity check. |
| [Donchian channel breakout](./src/main/java/com/alphatica/alis/examples/donchian) | Channel breakout, with and without parameter optimization. |
| [Williams %R](./src/main/java/com/alphatica/alis/examples/williamsr) | Williams %R indicator, with optimization. |
