# Stooq.pl data loader

Loading stooq data is fairly straightforward. Download data from https://stooq.pl/db/h/
and pick Dzienne -> ASCII / Polska. Unpack to directory Alphatica -> stooq_gpw.
You should have the following directory structure in your home directory:

```
Alphatica/stooq_gpw/stooq_data
Alphatica/stooq_gpw/stooq_data/data
Alphatica/stooq_gpw/stooq_data/data/daily
Alphatica/stooq_gpw/stooq_data/data/daily/pl
...
Alphatica/stooq_gpw/stooq_data/data/daily/pl/wse indices
Alphatica/stooq_gpw/stooq_data/data/daily/pl/wse indices indicators
...
Alphatica/stooq_gpw/stooq_data/data/daily/pl/wse stocks indicators
...
Alphatica/stooq_gpw/stooq_data/data/daily/pl/wse stocks
```

Then, data can be loaded up with:

```java
private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";
MarketData standardMarketData = StooqLoader.load(WORK_DIR);
```

Alternatively, you can unzip downloaded file straight from your 'Download' directory:

```java
    private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";
// Check for new data in "Downloads" dir in user's home directory. If file is not found, then do nothing.   
    StooqLoader.

unzipNew(WORK_DIR, "Downloads");

// Work with new data:
MarketData standardMarketData = StooqLoader.load(WORK_DIR);
```