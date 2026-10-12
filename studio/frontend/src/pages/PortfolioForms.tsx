// Portfolio → Add trade / Deposit / Withdraw cash / Import from file.
// The backend publishes portfolio-changed after saving, so the portfolio reloads by itself.

import { createSignal, type JSX, Show } from 'solid-js';
import { api, type ErrorMessage, toErrorMessage } from '../api';
import { Modal } from '../components/Modal';
import { StableLabel } from '../components/StableLabel';
import { ReportButton, showError } from '../toasts';

function today() {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`;
}

/** Dialog open on the portfolio screen; opened from the Portfolio menu in the top bar. */
export const [portfolioDialog, setPortfolioDialog] = createSignal<'trade' | 'cash'>();

/**
 * Lets the user choose a text file with account actions (one per line, as in the account file) and adds them
 * to the portfolio.
 */
export function importPortfolioFile() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '.csv,.txt,text/csv,text/plain';
  input.addEventListener('change', () => {
    const chosen = input.files?.[0];
    if (chosen) {
      api.upload('/api/portfolio/import', chosen).catch(showError);
    }
  });
  input.click();
}

// Kept outside the components: time and commission stay for the next trade, a half-filled form survives closing.
const [time, setTime] = createSignal(today());
const [market, setMarket] = createSignal('');
const [direction, setDirection] = createSignal<'buy' | 'sell'>('buy');
const [price, setPrice] = createSignal('');
const [quantity, setQuantity] = createSignal('');
const [commission, setCommission] = createSignal('0');
const [cashType, setCashType] = createSignal<'deposit' | 'withdrawal'>('deposit');
const [amount, setAmount] = createSignal('');

function TextField(props: { id: string; label: string; value: string; onInput: (value: string) => void } & JSX.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <>
      <label for={props.id}>{props.label}</label>
      <input
        id={props.id}
        type="text"
        inputmode={props.inputmode}
        placeholder={props.placeholder}
        autofocus={props.autofocus}
        value={props.value}
        onInput={(event) => props.onInput(event.currentTarget.value)}
      />
    </>
  );
}

/** Form in a dialog: sends the request, closes on success. Errors are shown in the dialog: toasts would be under its backdrop. */
function DialogForm(props: {
  submitLabel: string;
  onCancel: () => void;
  submit: () => Promise<void>;
  children: JSX.Element;
}) {
  const [saving, setSaving] = createSignal(false);
  const [error, setError] = createSignal<ErrorMessage>();
  return (
    <form
      onSubmit={async (event) => {
        event.preventDefault();
        setSaving(true);
        setError(undefined);
        try {
          await props.submit();
        } catch (e) {
          setError(toErrorMessage(e));
        } finally {
          setSaving(false);
        }
      }}
    >
      <div class="form-grid">{props.children}</div>
      <Show when={error()}>
        {(e) => (
          <div class="dialog-error" role="alert">
            <div class="toast-title">{e().title}</div>
            <div class="toast-message">{e().message}</div>
            <Show when={e().reportId}>
              {(reportId) => (
                <div class="toast-actions">
                  <ReportButton reportId={reportId()} />
                </div>
              )}
            </Show>
          </div>
        )}
      </Show>
      <div class="actions dialog-actions">
        <button class="btn" type="button" onClick={() => props.onCancel()}>
          Cancel
        </button>
        <button class="btn btn-primary" type="submit" disabled={saving()}>
          <StableLabel labels={[props.submitLabel, 'Saving…']} current={saving() ? 1 : 0} />
        </button>
      </div>
    </form>
  );
}

export function AddTradeDialog(props: { open: boolean; onClose: () => void }) {
  const submit = async () => {
    await api.post('/api/portfolio/trade', {
      time: time(),
      market: market(),
      direction: direction(),
      price: price(),
      quantity: quantity(),
      commission: commission(),
    });
    setMarket('');
    setPrice('');
    setQuantity('');
    props.onClose();
  };
  return (
    <Modal open={props.open} onClose={props.onClose} title="Add trade">
      <DialogForm submitLabel="Add" onCancel={props.onClose} submit={submit}>
        <TextField id="trade-time" label="Time" placeholder="yyyymmdd" inputmode="numeric" value={time()} onInput={setTime} />
        <TextField id="trade-market" label="Market" autofocus value={market()} onInput={setMarket} />
        <label for="trade-direction">Order</label>
        <select
          id="trade-direction"
          value={direction()}
          onChange={(event) => setDirection(event.currentTarget.value as 'buy' | 'sell')}
        >
          <option value="buy">Buy</option>
          <option value="sell">Sell</option>
        </select>
        <TextField id="trade-price" label="Price" inputmode="decimal" value={price()} onInput={setPrice} />
        <TextField id="trade-quantity" label="Quantity" inputmode="numeric" value={quantity()} onInput={setQuantity} />
        <TextField id="trade-commission" label="Commission" inputmode="decimal" value={commission()} onInput={setCommission} />
      </DialogForm>
    </Modal>
  );
}

export function CashDialog(props: { open: boolean; onClose: () => void }) {
  const submit = async () => {
    await api.post('/api/portfolio/cash', { time: time(), type: cashType(), amount: amount() });
    setAmount('');
    props.onClose();
  };
  return (
    <Modal open={props.open} onClose={props.onClose} title="Deposit / Withdraw cash">
      <DialogForm submitLabel="Submit" onCancel={props.onClose} submit={submit}>
        <TextField id="cash-time" label="Time" placeholder="yyyymmdd" inputmode="numeric" value={time()} onInput={setTime} />
        <label for="cash-type">Type</label>
        <select
          id="cash-type"
          value={cashType()}
          onChange={(event) => setCashType(event.currentTarget.value as 'deposit' | 'withdrawal')}
        >
          <option value="deposit">Deposit</option>
          <option value="withdrawal">Withdrawal</option>
        </select>
        <TextField id="cash-amount" label="Amount" inputmode="decimal" autofocus value={amount()} onInput={setAmount} />
      </DialogForm>
    </Modal>
  );
}
