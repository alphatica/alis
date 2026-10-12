// The single WebSocket to the backend. Messages: {topic, event, data}.
// Topic "app" (state changes) is received by everyone; other topics, e.g. "job/<id>", after subscribing.
// Reconnects automatically; after a reconnect subscriptions are restored and onReconnect listeners
// refresh data, because events may have been missed in the meantime.

type Message = { topic: string; event: string; data: unknown };
type Handler = (event: string, data: unknown) => void;

const APP_TOPIC = 'app';
const MAX_RETRY_DELAY_MS = 2000;

const handlers = new Map<string, Set<Handler>>();
const reconnectListeners = new Set<() => void>();
let socket: WebSocket | null = null;
let stopped = false;
let everConnected = false;
let retryDelay = 100;

export function connectLive() {
  if (!socket && !stopped) {
    open();
  }
}

/** After quitting: close and do not reconnect. */
export function stopLive() {
  stopped = true;
  socket?.close();
}

function open() {
  const ws = new WebSocket(`${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`);
  socket = ws;
  ws.onopen = () => {
    retryDelay = 100;
    for (const topic of handlers.keys()) {
      if (topic !== APP_TOPIC) {
        send('subscribe', topic);
      }
    }
    if (everConnected) {
      reconnectListeners.forEach((listener) => listener());
    }
    everConnected = true;
  };
  ws.onmessage = (event) => {
    const message = JSON.parse(event.data as string) as Message;
    handlers.get(message.topic)?.forEach((handler) => handler(message.event, message.data));
  };
  ws.onclose = () => {
    socket = null;
    if (!stopped) {
      setTimeout(open, retryDelay);
      retryDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY_MS);
    }
  };
}

function send(op: 'subscribe' | 'unsubscribe', topic: string) {
  if (socket?.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ op, topic }));
  }
}

/** @returns function cancelling the subscription */
export function subscribe(topic: string, handler: Handler): () => void {
  let topicHandlers = handlers.get(topic);
  if (!topicHandlers) {
    topicHandlers = new Set();
    handlers.set(topic, topicHandlers);
    if (topic !== APP_TOPIC) {
      send('subscribe', topic);
    }
  }
  topicHandlers.add(handler);
  const subscribed = topicHandlers;
  return () => {
    subscribed.delete(handler);
    if (subscribed.size === 0 && handlers.get(topic) === subscribed) {
      handlers.delete(topic);
      if (topic !== APP_TOPIC) {
        send('unsubscribe', topic);
      }
    }
  };
}

export function onAppEvent(name: string, handler: (data: unknown) => void): () => void {
  return subscribe(APP_TOPIC, (event, data) => {
    if (event === name) {
      handler(data);
    }
  });
}

export function onReconnect(listener: () => void) {
  reconnectListeners.add(listener);
}
