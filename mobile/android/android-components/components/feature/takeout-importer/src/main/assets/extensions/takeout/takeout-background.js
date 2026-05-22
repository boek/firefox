/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

const PORT_NAME = "mozacTakeoutImporter";

let nativePort = null;

function ensureNativePort() {
  if (nativePort) {
    return nativePort;
  }
  nativePort = browser.runtime.connectNative(PORT_NAME);
  nativePort.onDisconnect.addListener(() => {
    nativePort = null;
  });
  return nativePort;
}

function forward(message) {
  try {
    ensureNativePort().postMessage(message);
  } catch (e) {
    console.error("takeout-importer: failed to forward message", e);
  }
}

browser.runtime.onMessage.addListener((message, sender) => {
  if (!message || typeof message !== "object") {
    return false;
  }
  forward({
    name: message.name || "unknown",
    tabId: sender?.tab?.id ?? null,
    url: sender?.tab?.url ?? null,
    data: message.data ?? null,
  });
  return false;
});
