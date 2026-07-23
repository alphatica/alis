# Alis - Algorithmic Investing Software

Alis consists of two main parts:

- **Library**: A collection of basic building blocks designed to help with portfolio management, testing, and market analysis.
- **Studio**: A standalone desktop application for managing daily trading and research activities.

## Main Features

- Portfolio statistics
- Strategy backtesting
- Drawdown analysis
- Strategy parameter optimization
- Alpha verification
- Data mining for portfolio and strategy improvements

## Signal research workflow

`SignalExecutor` separates signal discovery from portfolio-capacity selection. A single execution
produces an immutable `SignalExecutionResult` containing every hypothetical `TradeOpportunity`.
The same result can then be replayed with `AllocationReplayer` for any positive `maxAllocation`
using either `STOP_ON_FIRST_REJECTION` or `PARTIAL_LAST_POSITION`.

Buy signals return `Optional<BuySignal>`. `requestedAllocation` is a normalized capital unit, while
`priority` is used only to rank simultaneous candidates. Scoring is performed separately through a
stateless `ScoreCalculator`; the library provides allocation-weighted profit-per-bar and
capacity-adjusted calculators. `AllocationScorer` can calculate a curve for many allocation limits
without evaluating indicators and signals again.

## Current Version

The current version is a Minimum Viable Product (MVP). GitHub star is much appreciated!

## Bug Reporting and Feature Requests

If you encounter any bugs, please report them to [lw@alphatica.com](mailto:lw@alphatica.com).  
Feature requests are welcome at [https://alphatica.com/feature-request/new/](https://alphatica.com/feature-request/new/).

## More Information

Visit [https://alphatica.com/](https://alphatica.com/) for additional details and market research.
