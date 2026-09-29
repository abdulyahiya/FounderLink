import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import tokenService from '../../core/tokenService';
import { Notification } from '../../types';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8086/ws';

/**
 * Subscribes to /topic/notifications/{userId} over STOMP/SockJS.
 * Calls onNotification(parsedPayload) whenever a new notification arrives.
 * Gracefully no-ops if the WebSocket endpoint is unavailable.
 */
const useNotificationSocket = (
  userId: number | undefined,
  onNotification: (_payload: Notification) => void
): void => {
  const clientRef = useRef<Client | null>(null);
  const onNotificationRef = useRef<(_payload: Notification) => void>(onNotification);

  useEffect(() => {
    onNotificationRef.current = onNotification;
  }, [onNotification]);

  useEffect(() => {
    if (!userId) return;

    const token = tokenService.getToken();

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 10000,
      onConnect: () => {
        client.subscribe(`/topic/notifications/${userId}`, (msg) => {
          try {
            const payload: Notification = JSON.parse(msg.body);
            onNotificationRef.current(payload);
          } catch (_err) {
            // malformed message — ignore
          }
        });
      },
      onStompError: () => {
        // Silently fail — initial fetch in Navbar already shows correct count
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current?.active) {
        clientRef.current.deactivate();
      }
    };
  }, [userId]);
};

export default useNotificationSocket;
