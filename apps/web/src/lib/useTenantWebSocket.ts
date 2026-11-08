import { useEffect, useRef, useState, useCallback } from "react";

export type WebSocketStatus = "connecting" | "connected" | "disconnected" | "reconnecting" | "failed";

export interface TenantWebSocketOptions<T> {
  url: string;
  enabled?: boolean;
  onMessage?: (data: T) => void;
  onError?: (error: Event) => void;
  maxReconnectDelayMs?: number;
  heartbeatIntervalMs?: number;
  fallbackPollingIntervalMs?: number;
  onFallbackPoll?: () => Promise<void> | void;
}

/**
 * useTenantWebSocket provides a memory-leak-safe, resilient WebSocket client
 * with exponential backoff, browser online/offline awareness, and automatic fallback polling.
 */
export function useTenantWebSocket<T = unknown>({
  url,
  enabled = true,
  onMessage,
  onError,
  maxReconnectDelayMs = 30000,
  heartbeatIntervalMs = 25000,
  fallbackPollingIntervalMs = 5000,
  onFallbackPoll
}: TenantWebSocketOptions<T>) {
  const [status, setStatus] = useState<WebSocketStatus>("disconnected");
  const [lastMessage, setLastMessage] = useState<T | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectAttemptRef = useRef<number>(0);
  const reconnectTimerRef = useRef<NodeJS.Timeout | null>(null);
  const heartbeatTimerRef = useRef<NodeJS.Timeout | null>(null);
  const pollingTimerRef = useRef<NodeJS.Timeout | null>(null);
  const isMountedRef = useRef<boolean>(true);

  // Keep latest callbacks in refs to prevent unnecessary re-connections on state updates
  const onMessageRef = useRef(onMessage);
  const onErrorRef = useRef(onError);
  const onFallbackPollRef = useRef(onFallbackPoll);

  useEffect(() => {
    onMessageRef.current = onMessage;
    onErrorRef.current = onError;
    onFallbackPollRef.current = onFallbackPoll;
  });

  const clearAllTimers = useCallback(() => {
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
    if (heartbeatTimerRef.current) {
      clearInterval(heartbeatTimerRef.current);
      heartbeatTimerRef.current = null;
    }
    if (pollingTimerRef.current) {
      clearInterval(pollingTimerRef.current);
      pollingTimerRef.current = null;
    }
  }, []);

  const startFallbackPolling = useCallback(() => {
    if (!onFallbackPollRef.current || pollingTimerRef.current) return;
    
    // Immediate first poll
    onFallbackPollRef.current();

    pollingTimerRef.current = setInterval(() => {
      if (isMountedRef.current && onFallbackPollRef.current) {
        onFallbackPollRef.current();
      }
    }, fallbackPollingIntervalMs);
  }, [fallbackPollingIntervalMs]);

  const stopFallbackPolling = useCallback(() => {
    if (pollingTimerRef.current) {
      clearInterval(pollingTimerRef.current);
      pollingTimerRef.current = null;
    }
  }, []);

  const connect = useCallback(() => {
    if (!enabled || !url || !isMountedRef.current) return;

    clearAllTimers();

    try {
      setStatus((prev) => (prev === "connected" ? "connected" : "connecting"));

      const ws = new WebSocket(url);
      wsRef.current = ws;

      ws.onopen = () => {
        if (!isMountedRef.current) {
          ws.close();
          return;
        }
        reconnectAttemptRef.current = 0;
        setStatus("connected");
        stopFallbackPolling();

        // Heartbeat ping/pong keeps connection alive across NAT/firewalls
        heartbeatTimerRef.current = setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ type: "ping", timestamp: Date.now() }));
          }
        }, heartbeatIntervalMs);
      };

      ws.onmessage = (event: MessageEvent) => {
        if (!isMountedRef.current) return;
        try {
          const parsed = JSON.parse(event.data) as T;
          setLastMessage(parsed);
          onMessageRef.current?.(parsed);
        } catch {
          // Non-JSON or pong frame
        }
      };

      ws.onerror = (event: Event) => {
        if (!isMountedRef.current) return;
        onErrorRef.current?.(event);
      };

      ws.onclose = (event: CloseEvent) => {
        if (!isMountedRef.current) return;

        if (heartbeatTimerRef.current) {
          clearInterval(heartbeatTimerRef.current);
          heartbeatTimerRef.current = null;
        }

        // Clean close initiated intentionally
        if (event.code === 1000) {
          setStatus("disconnected");
          return;
        }

        setStatus("reconnecting");
        startFallbackPolling();

        // Exponential backoff: 1s, 2s, 4s, 8s, up to maxReconnectDelayMs
        const attempt = reconnectAttemptRef.current;
        const delay = Math.min(1000 * Math.pow(2, attempt), maxReconnectDelayMs);
        reconnectAttemptRef.current += 1;

        reconnectTimerRef.current = setTimeout(() => {
          if (isMountedRef.current) {
            connect();
          }
        }, delay);
      };
    } catch {
      setStatus("failed");
      startFallbackPolling();
    }
  }, [enabled, url, clearAllTimers, stopFallbackPolling, startFallbackPolling, heartbeatIntervalMs, maxReconnectDelayMs]);

  useEffect(() => {
    isMountedRef.current = true;
    connect();

    // Handle system waking up or network re-attaching
    const handleOnline = () => {
      if (isMountedRef.current && status !== "connected") {
        reconnectAttemptRef.current = 0;
        connect();
      }
    };

    window.addEventListener("online", handleOnline);

    return () => {
      isMountedRef.current = false;
      window.removeEventListener("online", handleOnline);
      clearAllTimers();
      if (wsRef.current) {
        wsRef.current.close(1000, "Component unmounted");
        wsRef.current = null;
      }
    };
  }, [connect, clearAllTimers, status]);

  const send = useCallback((payload: unknown) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(typeof payload === "string" ? payload : JSON.stringify(payload));
      return true;
    }
    return false;
  }, []);

  return {
    status,
    lastMessage,
    send,
    reconnect: connect
  };
}
