type EventHandler = (data: any) => any | Promise<any>;

// javascript
let ws = null; // will be created by connectWebSocket()
const pending: Map<string, any> = new Map(); // id -> { resolve, reject, timer }
const handlers: Map<string, EventHandler> = new Map(); // eventType -> handler(data) -> (value|Promise)


let timer = null;

// helper to attach message/error/close handlers to a socket
function setupSocketHandlers(socket) {
    socket.onmessage = (event) => {
        console.debug("SERVER MESSAGE:", event.data);
        let msg;
        try {
            msg = JSON.parse(event.data);
        } catch (e) {
            console.debug("Non-JSON message from server:", event.data, e);
            return;
        }

        const id = msg.id;
        const eventType = msg.event;
        const status = msg.status;
        const data = msg.data;

        if (id && typeof status === "string") {
            const entry = pending.get(id);
            if (!entry) return;
            clearTimeout(entry.timer);
            pending.delete(id);

            if (status === "success") {
                entry.resolve(data === undefined ? {} : data);
            } else {
                entry.reject(new Error(msg.error || "Server returned error"));
            }
            return;
        }

        if (id && eventType) {
            const handler = handlers.get(eventType);
            const sendResponse = (result) => {
                const response = {id: id, event: eventType, status: result};
                if (socket && socket.readyState === WebSocket.OPEN) {
                    socket.send(JSON.stringify(response));
                } else {
                    console.warn("Cannot send response, websocket not open");
                }
            };

            if (!handler) {
                // no handler: respond with an error object in data so server can fail it
                sendResponse({error: "No handler for event: " + eventType});
                return;
            }

            try {
                Promise.resolve(handler(data === undefined ? {} : data))
                    .then(_ => sendResponse("success"))
                    .catch(_ => sendResponse("error"));
            } catch (err) {
                sendResponse("error");
            }
        }
    };

    socket.onerror = (error) => {
        console.error("WebSocket Error:", error);
        if (timer) clearInterval(timer);
        for (const [id, entry] of pending.entries()) {
            clearTimeout(entry.timer);
            entry.reject(new Error("WebSocket error/closed"));
            pending.delete(id);
        }
        if (socket.readyState === WebSocket.OPEN) socket.close(1000, "Unexpected error.");
    };

    socket.onclose = (event) => {
        if (timer) clearInterval(timer);
        for (const [id, entry] of pending.entries()) {
            clearTimeout(entry.timer);
            entry.reject(new Error("WebSocket closed"));
            pending.delete(id);
        }
        if (event.wasClean) {
            console.log(`Connection closed cleanly, code=${event.code} reason=${event.reason}`);
        } else {
            console.warn('Connection died unexpectedly.');
        }
    };
}

// connect/disconnect helpers
function connectWebSocket(url = "ws://localhost:9000/websocket") {
    if (ws && ws.readyState === WebSocket.OPEN) return Promise.resolve();
    if (ws && ws.readyState === WebSocket.CONNECTING) {
        // already connecting - return a promise that resolves on open
        return new Promise((resolve, reject) => {
            const prevOnOpen = ws.onopen;
            const prevOnError = ws.onerror;
            ws.onopen = (ev) => {
                if (prevOnOpen) prevOnOpen(ev);
                resolve();
            };
            ws.onerror = (err) => {
                if (prevOnError) prevOnError(err);
                reject(err);
            };
        });
    }

    ws = new WebSocket(url);
    setupSocketHandlers(ws);

    return new Promise((resolve, reject) => {
        ws.onopen = () => {
            console.log("WebSocket connection established!");
            // start heartbeat
            timer = setInterval(() => {
                if (ws && ws.readyState === WebSocket.OPEN) {
                    sendEventAndWait("ping", {}).then(
                        () => console.debug("PING RESPONSE RECEIVED"),
                    ).catch(
                        (err) => console.warn("PING ERROR:", err.message),
                    );
                    console.debug("PING SENT");
                }
            }, 5000);
            resolve();
        };

        ws.onerror = (err) => {
            reject(err);
        };
    });
}

function disconnectWebSocket(code = 1000, reason = "Client disconnect") {
    if (timer) {
        clearInterval(timer);
        timer = null;
    }
    if (ws) {
        try {
            ws.close(code, reason);
        } catch (e) {
        }
        ws = null;
    }
}

function sendEvent(eventType, eventData) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        console.warn("WebSocket is not open. Unable to send message.");
        return;
    }
    const id = Date.now().toString(36) + Math.random().toString(36).substring(2, 9);
    const message = {id: id, event: eventType, data: eventData};
    ws.send(JSON.stringify(message));
    console.debug("SENT:", message);
}

function sendEventAndWait(eventType, eventData, timeoutMs = 10000) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        return Promise.reject(new Error("WebSocket is not open"));
    }
    const id = Date.now().toString(36) + Math.random().toString(36).substring(2, 9);
    const message = {id: id, event: eventType, data: eventData};
    const p = new Promise((resolve, reject) => {
        const timerId = setTimeout(() => {
            if (pending.has(id)) {
                pending.delete(id);
                reject(new Error(`No response within ${timeoutMs}ms for id=${id}`));
            }
        }, timeoutMs);
        pending.set(id, {resolve, reject, timer: timerId});
    });
    ws.send(JSON.stringify(message));
    console.debug("SENT (await):", message);
    return p;
}

function onEvent(eventType: string, handler: EventHandler) {
    handlers.set(eventType, handler);
}

globalThis.sendEvent = sendEvent;
globalThis.sendEventAndWait = sendEventAndWait;
globalThis.onEvent = onEvent;
globalThis.connectWebSocket = connectWebSocket;
globalThis.disconnectWebSocket = disconnectWebSocket;
globalThis.isWebSocketConnected = () => !!ws && ws.readyState === WebSocket.OPEN;
