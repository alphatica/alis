import { Show } from 'solid-js';
import type { ErrorMessage } from '../api';
import { ReportButton } from '../toasts';

/** An error shown in place, e.g. in a dialog or as the status of the last job, instead of a toast. */
export function ErrorBox(props: { error: ErrorMessage; heading?: string }) {
  return (
    <div class="dialog-error" role="alert">
      <div class="toast-title">{props.heading ? `${props.heading}: ${props.error.title}` : props.error.title}</div>
      <div class="toast-message">{props.error.message}</div>
      <Show when={props.error.reportId}>
        {(reportId) => (
          <div class="toast-actions">
            <ReportButton reportId={reportId()} />
          </div>
        )}
      </Show>
    </div>
  );
}

/** Status of a job that ended with an error; stays until the next start. */
export function JobFailed(props: { error: ErrorMessage | undefined }) {
  return <Show when={props.error}>{(error) => <ErrorBox error={error()} heading="Last run failed" />}</Show>;
}
