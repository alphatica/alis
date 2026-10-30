import { showError } from '../toasts';
import { Modal } from './Modal';

/** Parameters of a strategy as Java code, one field per line, with Copy. Open while `params` is set. */
export function ParamsDialog(props: { params: string[] | undefined; title: string; onClose: () => void }) {
  const text = () => props.params?.join('\n') ?? '';
  const copy = () => navigator.clipboard.writeText(text()).catch(showError);
  return (
    <Modal open={props.params !== undefined} onClose={props.onClose} title={props.params ? props.title : ''}>
      <pre class="job-output">{text()}</pre>
      <div class="actions dialog-actions">
        <button class="btn" type="button" onClick={copy}>
          Copy
        </button>
        <button class="btn" type="button" onClick={() => props.onClose()}>
          Close
        </button>
      </div>
    </Modal>
  );
}
