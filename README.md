# ALIS :: Algorithmic Investing Software

Alis is a Java library that helps to analyze financial markets.
More about using Alis can be found at [Alphatica.com](https://alphatica.com)

## Features

### Loading data

[Loading Stooq.pl data](alis/src/main/java/com/alphatica/alis/data/loader/stooq/README.md)

### Analyse market prices

[Checks for the most recent data](alis-examples/CurrentChecks/README.md)

### Simulated trading for trading strategies

[Simulate trading with mechanical systems](alis-examples/StrategiesTests/README.md)

### Charts

You can easily generate reasonably looking charts. Consult [examples](alis-examples/README.md) to see how.

## Documentation

The source code is the best documentation. Check [examples](alis-examples/README.md)
to see what can be done.

## Installation

At the moment, only installation from source is possible

```bash
git clone https://github.com/alphatica/alis.git
cd alis
mvn install
```

Then you can use Alis as normal maven dependency:

    <dependencies>
        <dependency>
            <groupId>com.alphatica</groupId>
            <artifactId>alis</artifactId>
            <version>...</version>
        </dependency>
    </dependencies>
