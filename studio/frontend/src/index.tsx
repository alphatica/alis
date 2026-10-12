import { Navigate, Route, Router } from '@solidjs/router';
import { type Component, createSignal, Match, onMount, Switch } from 'solid-js';
import { render } from 'solid-js/web';
import { api } from './api';
import './app.css';
import { ContextMenu } from './components/ContextMenu';
import { Layout, SCREENS, Shell } from './components/Layout';
import { connectLive, stopLive } from './live';
import { Backtest } from './pages/Backtest';
import { BetterExits } from './pages/BetterExits';
import { Data } from './pages/Data';
import { Help } from './pages/Help';
import { Optimization } from './pages/Optimization';
import { Portfolio } from './pages/Portfolio';
import { Signals } from './pages/Signals';
import { Closed, License, NotFound, Placeholder } from './pages/Simple';
import { loadAll, setDataStatus, setStudioName } from './state';
import { showError, Toasts } from './toasts';

type Phase = 'starting' | 'license' | 'ready' | 'closed';

const PAGES: Record<string, Component> = {
  '/portfolio': Portfolio,
  '/signals': Signals,
  '/backtest': Backtest,
  '/optimization': Optimization,
  '/better-exits': BetterExits,
  '/data': Data,
  '/help': Help,
};

function App() {
  const [phase, setPhase] = createSignal<Phase>('starting');
  const [closedMessage, setClosedMessage] = createSignal('');

  const start = () => {
    connectLive();
    loadAll();
    setPhase('ready');
  };

  const close = (message: string) => {
    stopLive();
    setClosedMessage(message);
    setPhase('closed');
  };

  const quit = () => {
    if (confirm('Quit Alis Studio?')) {
      api
        .post('/api/shutdown')
        .then(() => close('Alis Studio has been closed.'))
        .catch(showError);
    }
  };

  onMount(async () => {
    try {
      const status = await api.get<{ licenseAccepted: boolean; dataStatus: string; studioName: string }>('/api/app');
      setDataStatus(status.dataStatus ?? '');
      setStudioName(status.studioName);
      if (status.licenseAccepted) {
        start();
      } else {
        setPhase('license');
      }
    } catch (e) {
      showError(e);
    }
  });

  return (
    <>
      <Switch>
        <Match when={phase() === 'license'}>
          <Shell title="License">
            <License onAccept={start} onDecline={() => close('License declined. Alis Studio has been closed.')} />
          </Shell>
        </Match>
        <Match when={phase() === 'ready'}>
          <Router root={(props) => <Layout onQuit={quit}>{props.children}</Layout>}>
            <Route path="/" component={() => <Navigate href={SCREENS[0].path} />} />
            {SCREENS.map((item) => (
              <Route path={item.path} component={PAGES[item.path] ?? Placeholder} />
            ))}
            <Route path="*" component={NotFound} />
          </Router>
        </Match>
        <Match when={phase() === 'closed'}>
          <Shell title="Closed">
            <Closed message={closedMessage()} />
          </Shell>
        </Match>
      </Switch>
      <ContextMenu />
      <Toasts />
    </>
  );
}

render(() => <App />, document.getElementById('root')!);
