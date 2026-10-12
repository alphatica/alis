import { createSignal, For, Show } from 'solid-js';
import { api } from '../api';
import { Loaded } from '../components/Loaded';
import { Modal } from '../components/Modal';
import { StableLabel } from '../components/StableLabel';
import { markets, trackJob } from '../state';
import { PagedTable } from '../table';
import { showError } from '../toasts';

type DirectoryListing = {
  path: string;
  parent: string | null;
  directories: { name: string; path: string }[];
};

// Kept outside the component, so the state is still there after switching screens.
/** Directory shown in the browser; the server starts in the user's home directory. */
const [listing, setListing] = createSignal<DirectoryListing>();
const [browserOpen, setBrowserOpen] = createSignal(false);
/** True while market data is being loaded: the markets table is hidden and the Load buttons are off until it is done. */
const [loading, setLoading] = createSignal(false);
/** URL of the running load, so its button can say "Loading…". */
const [loadingUrl, setLoadingUrl] = createSignal<string>();

async function start(url: string, body?: unknown, onDone?: () => void) {
  try {
    const { jobId } = await api.post<{ jobId: string }>(url, body);
    trackJob(jobId, onDone);
  } catch (e) {
    showError(e);
    onDone?.();
  }
}

/** Loading replaces the markets: the old table is cleared at once and the new one shown when loading ends. */
function load(url: string, body?: unknown) {
  markets.clear();
  setLoading(true);
  setLoadingUrl(url);
  start(url, body, () => {
    setLoading(false);
    setLoadingUrl(undefined);
    markets.load();
  });
}

async function browse(path?: string) {
  try {
    const query = path === undefined ? '' : `?path=${encodeURIComponent(path)}`;
    setListing(await api.get<DirectoryListing>(`/api/data/directories${query}`));
  } catch (e) {
    showError(e);
  }
}

function openBrowser() {
  setBrowserOpen(true);
  browse(listing()?.path);
}

function loadDirectory(path: string) {
  setBrowserOpen(false);
  // Never two loads at once
  if (!loading()) {
    load('/api/data/load-pl-directory', { directory: path });
  }
}

function DirectoryBrowser() {
  return (
    <Modal open={browserOpen()} onClose={() => setBrowserOpen(false)} title="Load PL from directory">
      <div class="dir-browser">
        <div class="dir-current">
          <button
            class="btn"
            type="button"
            disabled={!listing()?.parent}
            onClick={() => browse(listing()!.parent!)}
          >
            ↑ Up
          </button>
          <code class="dir-path">{listing()?.path}</code>
        </div>
        <ul class="dir-list">
          <For each={listing()?.directories} fallback={<li class="dir-empty muted">No subdirectories</li>}>
            {(entry) => (
              <li class="dir-row">
                <button class="dir-name" type="button" onClick={() => browse(entry.path)}>
                  📁 {entry.name}
                </button>
                <button class="btn btn-primary" type="button" onClick={() => loadDirectory(entry.path)}>
                  Load
                </button>
              </li>
            )}
          </For>
        </ul>
        <div class="toolbar dialog-actions">
          <button class="btn" type="button" onClick={() => setBrowserOpen(false)}>
            Cancel
          </button>
        </div>
      </div>
    </Modal>
  );
}

export function Data() {
  return (
    <div class="page-fill">
      <div class="toolbar">
        <a class="btn" href="https://stooq.pl/db/h/" target="_blank" rel="noopener">
          Open Stooq.pl ↗
        </a>
        <button class="btn" type="button" onClick={() => start('/api/data/unzip')}>
          Unzip downloaded
        </button>
        <button class="btn btn-primary" type="button" disabled={loading()} onClick={() => load('/api/data/load-pl')}>
          <StableLabel labels={['Load PL', 'Loading…']} current={loadingUrl() === '/api/data/load-pl' ? 1 : 0} />
        </button>
        <button class="btn btn-primary" type="button" disabled={loading()} onClick={() => load('/api/data/load-us')}>
          <StableLabel labels={['Load US', 'Loading…']} current={loadingUrl() === '/api/data/load-us' ? 1 : 0} />
        </button>
        <button class="btn" type="button" disabled={loading()} onClick={openBrowser}>
          <StableLabel
            labels={['Load PL from directory…', 'Loading…']}
            current={loadingUrl() === '/api/data/load-pl-directory' ? 1 : 0}
          />
        </button>
      </div>
      <DirectoryBrowser />
      <Show when={!loading()}>
        <Loaded store={markets}>{() => <PagedTable source={markets} />}</Loaded>
      </Show>
    </div>
  );
}
