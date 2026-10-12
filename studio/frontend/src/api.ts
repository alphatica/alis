// REST calls to the backend. Errors come back as ErrorMessage JSON and are shown as toasts.

export type ErrorMessage = {
  title: string;
  message: string;
  /** Set for unexpected errors: the user may choose to report them. */
  reportId: string | null;
};

export class ApiError extends Error {
  constructor(readonly error: ErrorMessage, readonly status: number) {
    super(error.message);
  }
}

export function toErrorMessage(e: unknown): ErrorMessage {
  if (e instanceof ApiError) {
    return e.error;
  }
  return { title: 'Unexpected error', message: String(e), reportId: null };
}

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError({ title: 'Connection error', message: 'Alis Studio is not responding.', reportId: null }, 0);
  }
  if (!response.ok) {
    throw new ApiError(await errorFrom(response), response.status);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function errorFrom(response: Response): Promise<ErrorMessage> {
  try {
    const body = await response.json();
    if (body && typeof body.message === 'string') {
      return { title: body.title ?? 'Error', message: body.message, reportId: body.reportId ?? null };
    }
    if (body && typeof body.title === 'string') {
      return { title: 'Error', message: body.title, reportId: null };
    }
  } catch {
    // Not JSON
  }
  return { title: 'Error', message: `${response.status} ${response.statusText}`, reportId: null };
}

export const api = {
  get: <T>(url: string) => request<T>('GET', url),
  post: <T = void>(url: string, body?: unknown) => request<T>('POST', url, body),
  /** Sends the file's content as the request body. */
  upload: async <T>(url: string, file: File): Promise<T> => {
    let response: Response;
    try {
      response = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: file });
    } catch {
      throw new ApiError({ title: 'Connection error', message: 'Alis Studio is not responding.', reportId: null }, 0);
    }
    if (!response.ok) {
      throw new ApiError(await errorFrom(response), response.status);
    }
    return (await response.json()) as T;
  },
};

/** Saves the response as a file in the browser's downloads; errors are thrown like the other calls. */
export async function download(url: string, fileName: string) {
  let response: Response;
  try {
    response = await fetch(url);
  } catch {
    throw new ApiError({ title: 'Connection error', message: 'Alis Studio is not responding.', reportId: null }, 0);
  }
  if (!response.ok) {
    throw new ApiError(await errorFrom(response), response.status);
  }
  const link = document.createElement('a');
  link.href = URL.createObjectURL(await response.blob());
  link.download = fileName;
  link.click();
  setTimeout(() => URL.revokeObjectURL(link.href), 0);
}
