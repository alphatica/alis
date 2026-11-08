import { createSignal, type JSX, onCleanup } from 'solid-js';
import { showError } from '../toasts';
import { Modal } from './Modal';

const FLASH_MS = 200;

/** Parameters of a strategy as Java code, one field per line, with Copy. Open while `params` is set. */
export function ParamsDialog(props: { params: string[] | undefined; title: string; onClose: () => void }) {
  const text = () => props.params?.join('\n') ?? '';
  const copy = () => navigator.clipboard.writeText(text()).catch(showError);
  return (
    <Modal open={props.params !== undefined} onClose={props.onClose} title={props.params ? props.title : ''}>
      <pre class="job-output">{text()}</pre>
      <div class="actions dialog-actions">
        <FlashButton onClick={copy}>Copy</FlashButton>
        <FlashButton onFlashEnd={() => props.onClose()}>Close</FlashButton>
      </div>
    </Modal>
  );
}

function FlashButton(props: { onClick?: () => void; onFlashEnd?: () => void; children: JSX.Element }) {
  const [flash, setFlash] = createSignal(false);
  let timer: ReturnType<typeof setTimeout> | undefined;
  onCleanup(() => clearTimeout(timer));
  const click = () => {
    clearTimeout(timer);
    setFlash(true);
    timer = setTimeout(() => {
      setFlash(false);
      props.onFlashEnd?.();
    }, FLASH_MS);
    props.onClick?.();
  };
  return (
    <button class="btn" classList={{ 'btn-flash': flash() }} type="button" onClick={click}>
      {props.children}
    </button>
  );
}
