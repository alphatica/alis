import { createSignal, Show } from 'solid-js';
import { EquityChart, type EquityMode, EquityModeSwitch } from '../components/EquityChart';
import { Loaded } from '../components/Loaded';
import { StatTiles } from '../components/StatTiles';
import { Tabs } from '../components/Tabs';
import { type PortfolioData, portfolio } from '../state';
import { TextTable } from '../table';
import { AddTradeDialog, CashDialog, portfolioDialog, setPortfolioDialog } from './PortfolioForms';

// Kept outside the component, so the selected tab is still there after switching screens.
const [tab, setTab] = createSignal(0);
const closeDialog = () => setPortfolioDialog(undefined);
const [equityMode, setEquityMode] = createSignal<EquityMode>('nav');

export function Portfolio() {
  return (
    <div class="page-fill">
      <AddTradeDialog open={portfolioDialog() === 'trade'} onClose={closeDialog} />
      <CashDialog open={portfolioDialog() === 'cash'} onClose={closeDialog} />
      <Loaded store={portfolio}>
        {(data) => (
          <div class="fill-column">
            <Tabs
              selected={tab()}
              onSelect={setTab}
              tabs={[
                {
                  label: 'Equity',
                  content: () => <Equity data={data()} />,
                  actions: () =>
                    hasEquity(data()) ? <EquityModeSwitch mode={equityMode()} onChange={setEquityMode} /> : undefined,
                },
                { label: 'Positions', content: () => <TextTable table={data().history} /> },
                { label: 'History', content: () => <TextTable table={data().actions} /> },
              ]}
            />
          </div>
        )}
      </Loaded>
    </div>
  );
}

const hasEquity = (data: PortfolioData) => data.equity.times.length > 0;

function Equity(props: { data: PortfolioData }) {
  return (
    <>
      <Show
        when={hasEquity(props.data)}
        fallback={
          <p class="muted">
            {props.data.actions.rows.length === 0 ? 'No account actions yet.' : 'Load market data to see the equity.'}
          </p>
        }
      >
        <EquityChart equity={props.data.equity} mode={equityMode()} stats={props.data.stats} fileName="portfolio" />
      </Show>
      <StatTiles stats={props.data.stats} />
    </>
  );
}
