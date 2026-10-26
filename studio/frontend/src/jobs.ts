// Long-running backend jobs (JobRegistry.java) followed by the screens. A job's output stays on the backend, also
// after it ends: when the GUI is opened again each screen finds its last job (GET /api/jobs) and shows it.
// On subscribing, the backend first sends a "snapshot" with everything the job sent so far, then new data.

import { type Accessor, createSignal } from 'solid-js';
import { api, type ErrorMessage, toErrorMessage } from './api';
import { subscribe } from './live';
import { pushToast, showError } from './toasts';

/** A job as listed by GET /api/jobs. */
type JobInfo = { id: string; type: string; input: unknown; running: boolean; cancelRequested: boolean };

/** Everything the job sent so far: all items of every append event, the latest value of every replace event. */
type Snapshot = { appends: Record<string, unknown[]>; replaces: Record<string, unknown> };

const ERROR_EVENT = 'error';

type WatchHandlers = {
  /** Clears the shown output: the snapshot that follows contains all of it again. */
  reset?: () => void;
  /** Output of the job: for append events a list of items, for replace events the value. */
  event?: (event: string, data: unknown) => void;
  /**
   * @param all every error of the job so far
   * @param fresh errors not reported before: the ones that happened while the GUI was closed are not fresh
   */
  errors: (all: ErrorMessage[], fresh: ErrorMessage[]) => void;
  done: () => void;
};

/**
 * Follows one job until it ends. Errors already seen are not reported again when the snapshot is sent again
 * after a reconnect.
 *
 * @param restored the job was found after opening the GUI: errors in its first snapshot are old, not fresh
 * @returns function ending the subscription
 */
export function watchJob(id: string, restored: boolean, handlers: WatchHandlers): () => void {
  let errors: ErrorMessage[] = [];
  let firstSnapshot = true;
  const unsubscribe = subscribe(`job/${id}`, (event, data) => {
    if (event === 'snapshot') {
      const snapshot = data as Snapshot;
      handlers.reset?.();
      for (const [name, items] of Object.entries(snapshot.appends)) {
        if (name !== ERROR_EVENT) {
          handlers.event?.(name, items);
        }
      }
      for (const [name, value] of Object.entries(snapshot.replaces)) {
        handlers.event?.(name, value);
      }
      const all = (snapshot.appends[ERROR_EVENT] ?? []) as ErrorMessage[];
      const fresh = restored && firstSnapshot ? [] : all.slice(errors.length);
      errors = all;
      firstSnapshot = false;
      handlers.errors(all, fresh);
    } else if (event === ERROR_EVENT) {
      const fresh = data as ErrorMessage[];
      errors = [...errors, ...fresh];
      handlers.errors(errors, fresh);
    } else if (event === 'done') {
      unsubscribe();
      handlers.done();
    } else {
      handlers.event?.(event, data);
    }
  });
  return unsubscribe;
}

export type JobView<Input> = {
  /** Clears the shown output: a new job starts, or the output so far of the followed job arrives again. */
  reset: () => void;
  /** Output of the job: for append events a list of items, for replace events the value. */
  event: (event: string, data: unknown) => void;
  /** Shows the settings of a job found after opening the GUI: the request it was started with. */
  restore?: (input: Input) => void;
  /** The last error of the followed job, shown on the screen; also after reopening the GUI. */
  failed?: (error: ErrorMessage) => void;
  /** Fresh errors, and errors of starting the job, become toasts unless false; then they go to `failed` only. */
  toastErrors?: boolean;
};

export type JobControl = {
  running: Accessor<boolean>;
  stopping: Accessor<boolean>;
  /** A job was started or found: its output (or "no results") is shown. */
  started: Accessor<boolean>;
  /** The followed job was found after opening the GUI, not started here: the form shows its settings. */
  restored: Accessor<boolean>;
  start: (request: () => Promise<{ jobId: string }>) => Promise<void>;
  stop: () => void;
};

const restorers = new Map<string, (job: JobInfo) => void>();

/** Shows the last job of every type on its screen. Called at start and after reconnecting. */
export function restoreJobs() {
  api.get<JobInfo[]>('/api/jobs').then(
    (jobs) => jobs.forEach((job) => restorers.get(job.type)?.(job)),
    () => {
      // The next reconnect tries again
    },
  );
}

/**
 * State of a screen's job of the given type (at most one runs per type). Created once per screen, outside the
 * component, so it survives switching screens.
 */
export function createJob<Input>(type: string, view: JobView<Input>): JobControl {
  const [followed, setFollowed] = createSignal<string>();
  const [running, setRunning] = createSignal(false);
  const [stopping, setStopping] = createSignal(false);
  const [restored, setRestored] = createSignal(false);
  let starting = false;
  let unsubscribe = () => {};

  function follow(id: string, isRestored: boolean) {
    unsubscribe();
    setFollowed(id);
    setRestored(isRestored);
    unsubscribe = watchJob(id, isRestored, {
      reset: view.reset,
      event: view.event,
      errors: (all, fresh) => {
        if (view.toastErrors !== false) {
          fresh.forEach(pushToast);
        }
        const last = all.at(-1);
        if (last) {
          view.failed?.(last);
        }
      },
      done: () => {
        setRunning(false);
        setStopping(false);
      },
    });
  }

  restorers.set(type, (job) => {
    // A job being started here is followed when the backend answers; the one followed already is up to date
    if (starting || job.id === followed()) {
      return;
    }
    view.reset();
    view.restore?.(job.input as Input);
    setRunning(job.running);
    setStopping(job.running && job.cancelRequested);
    follow(job.id, true);
  });

  async function start(request: () => Promise<{ jobId: string }>) {
    starting = true;
    unsubscribe();
    unsubscribe = () => {};
    setFollowed(undefined);
    setRestored(false);
    view.reset();
    setStopping(false);
    setRunning(true);
    try {
      const { jobId } = await request();
      follow(jobId, false);
    } catch (e) {
      if (view.toastErrors === false) {
        view.failed?.(toErrorMessage(e));
      } else {
        showError(e);
      }
      setRunning(false);
    } finally {
      starting = false;
    }
  }

  function stop() {
    const id = followed();
    if (id && running()) {
      setStopping(true);
      api.post(`/api/jobs/${id}/cancel`).catch(showError);
    }
  }

  return {
    running,
    stopping,
    started: () => running() || followed() !== undefined,
    restored,
    start,
    stop,
  };
}
