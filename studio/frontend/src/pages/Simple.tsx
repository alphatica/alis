import { useLocation } from '@solidjs/router';
import { onMount, createSignal } from 'solid-js';
import { api } from '../api';
import { labelOf } from '../components/Layout';
import { showError } from '../toasts';

/** Screens from the navigation that do not have a web version yet. */
export function Placeholder() {
  const location = useLocation();
  return (
    <>
      <h1>{labelOf(location.pathname)}</h1>
      <p class="muted">This screen is not available in the web version yet.</p>
    </>
  );
}

export function NotFound() {
  return (
    <>
      <h1>Not found</h1>
      <p class="muted">There is no such page.</p>
    </>
  );
}

export function License(props: { onAccept: () => void; onDecline: () => void }) {
  const [text, setText] = createSignal<string>();
  onMount(() =>
    api
      .get<{ text: string }>('/api/license')
      .then((license) => setText(license.text))
      .catch(showError),
  );
  const accept = () => api.post('/api/license/accept').then(props.onAccept).catch(showError);
  const decline = () => api.post('/api/license/decline').then(props.onDecline).catch(showError);
  return (
    <>
      <h1>License</h1>
      <p class="muted">Please read and accept the license to use Alis Studio.</p>
      <pre class="panel license-text">{text() ?? 'Loading…'}</pre>
      <div class="actions">
        <button class="btn btn-primary" onClick={accept} disabled={text() === undefined}>
          Accept
        </button>
        <button class="btn btn-danger" onClick={decline}>
          Decline
        </button>
      </div>
    </>
  );
}

export function Closed(props: { message: string }) {
  return (
    <>
      <h1>Alis Studio</h1>
      <p class="muted">{props.message}</p>
      <p class="muted">You can close this tab.</p>
    </>
  );
}
