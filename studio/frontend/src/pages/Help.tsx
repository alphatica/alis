import { createSignal, onMount } from 'solid-js';
import { api } from '../api';
import { Modal } from '../components/Modal';
import { studioName } from '../state';
import { showError } from '../toasts';

export const WEBSITE_URL = 'https://alphatica.com/en/software/alis/';
export const FEATURE_REQUEST_URL = 'https://alphatica.com/feature-request/new';

export const [helpDialog, setHelpDialog] = createSignal<'license' | 'about'>();
const closeDialog = () => setHelpDialog(undefined);

/** Help: website, license, feature request and about. */
export function Help() {
  return (
    <>
      <h1>Help</h1>
      <div class="panel help-list">
        <ExternalLink href={WEBSITE_URL}>Website</ExternalLink>
        <span class="muted">Alis on alphatica.com</span>
        <button class="btn" onClick={() => setHelpDialog('license')}>
          License…
        </button>
        <span class="muted">GNU Affero General Public License 3.0</span>
        <ExternalLink href={FEATURE_REQUEST_URL}>Feature request</ExternalLink>
        <span class="muted">Suggest a new feature</span>
        <button class="btn" onClick={() => setHelpDialog('about')}>
          About…
        </button>
        <span class="muted">Version and license</span>
      </div>
      <LicenseDialog open={helpDialog() === 'license'} onClose={closeDialog} />
      <AboutDialog open={helpDialog() === 'about'} onClose={closeDialog} />
    </>
  );
}

function ExternalLink(props: { href: string; children: string }) {
  return (
    <a class="btn" href={props.href} target="_blank" rel="noopener noreferrer">
      {props.children} ↗
    </a>
  );
}

function LicenseDialog(props: { open: boolean; onClose: () => void }) {
  return (
    <Modal open={props.open} onClose={props.onClose} title="License">
      <LicenseText />
      <CloseButton onClose={props.onClose} />
    </Modal>
  );
}

function LicenseText() {
  const [text, setText] = createSignal<string>();
  onMount(() =>
    api
      .get<{ text: string }>('/api/license')
      .then((license) => setText(license.text))
      .catch(showError),
  );
  return <pre class="panel license-text help-license">{text() ?? 'Loading…'}</pre>;
}

function AboutDialog(props: { open: boolean; onClose: () => void }) {
  return (
    <Modal open={props.open} onClose={props.onClose} title="About">
      <div class="about">
        <p>{studioName()}</p>
        <p>
          Web:{' '}
          <a href={WEBSITE_URL} target="_blank" rel="noopener noreferrer">
            alphatica.com
          </a>
        </p>
        <p>Licensed under GNU AFFERO GENERAL PUBLIC LICENSE 3.0</p>
      </div>
      <CloseButton onClose={props.onClose} />
    </Modal>
  );
}

function CloseButton(props: { onClose: () => void }) {
  return (
    <div class="actions dialog-actions">
      <button class="btn" autofocus onClick={() => props.onClose()}>
        Close
      </button>
    </div>
  );
}
