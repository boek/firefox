/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

(() => {
  const MESSAGE_NAME = "takeoutStep";

  function report(actionID, status, detail) {
    browser.runtime.sendMessage({
      name: MESSAGE_NAME,
      data: {
        result: {
          [status]: { actionID, ...(detail ? { detail } : {}) },
        },
      },
    });
  }

  const success = (actionID, detail) => report(actionID, "success", detail);
  const failure = (actionID, detail) => report(actionID, "error", detail);

  // Wait up to `timeoutMs` for an element matched by `selector` to appear.
  function waitForElement(
    selector,
    { timeoutMs = 15000, root = document } = {}
  ) {
    return new Promise((resolve, reject) => {
      const existing = root.querySelector(selector);
      if (existing) {
        resolve(existing);
        return;
      }
      const observer = new MutationObserver(() => {
        const el = root.querySelector(selector);
        if (el) {
          observer.disconnect();
          resolve(el);
        }
      });
      observer.observe(root.body || root, { childList: true, subtree: true });
      setTimeout(() => {
        observer.disconnect();
        reject(new Error(`timeout waiting for ${selector}`));
      }, timeoutMs);
    });
  }

  async function clickWhenReady(actionID, selector) {
    try {
      const el = await waitForElement(selector);
      el.click();
      success(actionID);
    } catch (e) {
      failure(actionID, String(e?.message || e));
      throw e;
    }
  }

  async function runBookmarkExportJourney() {
    if (location.host === "accounts.google.com") {
      success("login-detected");
      return;
    }
    if (location.host !== "takeout.google.com") {
      return;
    }

    success("takeout-loaded");

    // The selectors below are intentionally written as data-attribute hooks
    // so that they can be tuned against Takeout's current DOM without
    // touching the surrounding plumbing. Update these as Takeout evolves.
    try {
      await clickWhenReady(
        "deselect-all",
        '[data-takeout-action="deselect-all"]'
      );
      await clickWhenReady(
        "select-bookmarks",
        '[data-takeout-product="chrome-bookmarks"] input[type="checkbox"]'
      );
      await clickWhenReady("next-step", '[data-takeout-action="next-step"]');
      await clickWhenReady(
        "create-export",
        '[data-takeout-action="create-export"]'
      );
      await clickWhenReady(
        "download-export",
        '[data-takeout-action="download"]',
        { timeoutMs: 5 * 60 * 1000 }
      );
      success("download-triggered");
    } catch (_e) {
      // Per-step failure has already been reported by clickWhenReady.
    }
  }

  runBookmarkExportJourney();
})();
