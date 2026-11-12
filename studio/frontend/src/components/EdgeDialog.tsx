import { EdgeReport } from './EdgeReport';
import { Modal } from './Modal';

export function EdgeDialog(props: { open: boolean; url: string; onClose: () => void }) {
  return (
    <Modal open={props.open} onClose={props.onClose} title="Edge analysis">
      <div class="edge-dialog">
        <EdgeReport url={props.url} />
      </div>
      <div class="actions dialog-actions">
        <button class="btn" type="button" onClick={() => props.onClose()}>
          Close
        </button>
      </div>
    </Modal>
  );
}
