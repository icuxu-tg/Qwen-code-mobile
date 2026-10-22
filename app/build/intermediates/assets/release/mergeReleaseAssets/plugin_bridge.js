/* Мост Margelet-плагинов в Web Shell Qwen.
 *
 * on_send-фильтры работают синхронно: Java-хост ждёт ответ Python-плагина
 * до 450 мс. Если плагин вызвал margelet.dont_send(), отправка отменяется.
 * send() плагина отправляет сообщение в текущую открытую сессию Qwen.
 */
(function () {
  "use strict";
  if (window.__qwenMobilePluginBridge) {
    return;
  }
  window.__qwenMobilePluginBridge = true;

  var bridge = window.AndroidPlugins;
  var originalFetch = window.fetch;
  if (typeof originalFetch !== "function") {
    return;
  }

  function promptText(body) {
    if (!body || !Array.isArray(body.prompt)) {
      return null;
    }
    var parts = [];
    var first = -1;
    for (var i = 0; i < body.prompt.length; i++) {
      var block = body.prompt[i];
      if (block && block.type === "text" && typeof block.text === "string") {
        if (first < 0) {
          first = i;
        }
        parts.push(block.text);
      }
    }
    if (first < 0) {
      return null;
    }
    return {text: parts.join("\n"), first: first};
  }

  window.fetch = function (input, init) {
    try {
      var url = typeof input === "string" ? input : (input && input.url) || "";
      var method = ((init && init.method) || (input && input.method) || "GET")
        .toUpperCase();
      if (bridge && method === "POST"
          && /\/session\/[^/]+\/prompt(?:\?|$)/.test(url)
          && init && typeof init.body === "string") {
        var body = JSON.parse(init.body);
        var prompt = promptText(body);
        if (prompt && prompt.text) {
          var filtered = bridge.filterSend(prompt.text);
          if (filtered === null) {
            return Promise.reject(new Error(
              "Сообщение отменено on_send-плагином Margelet"));
          }
          filtered = String(filtered);
          if (filtered !== prompt.text) {
            body.prompt[prompt.first].text = filtered;
            for (var i = body.prompt.length - 1; i >= 0; i--) {
              if (i !== prompt.first && body.prompt[i]
                  && body.prompt[i].type === "text") {
                body.prompt.splice(i, 1);
                if (i < prompt.first) {
                  prompt.first--;
                }
              }
            }
            init = Object.assign({}, init, {body: JSON.stringify(body)});
          }
        }
      }
    } catch (e) {
      /* Ошибка моста не должна ломать чат. */
    }
    return originalFetch.call(this, input, init);
  };

  try {
    console.log("Qwen Mobile plugin bridge ready");
  } catch (e) {
    /* консоль может быть недоступна */
  }

  window.qwenMobileSend = function (chat, text) {
    var match = /^\/session\/([^/]+)/.exec(window.location.pathname);
    if (!match) {
      return false;
    }
    var sessionId = decodeURIComponent(match[1]);
    var body = {
      prompt: [{type: "text", text: String(text)}]
    };
    try {
      originalFetch.call(window,
        "/session/" + encodeURIComponent(sessionId) + "/prompt", {
          method: "POST",
          headers: {"Content-Type": "application/json"},
          body: JSON.stringify(body)
        }).catch(function () {
          /* Ошибка показывается самим Web Shell. */
        });
      return true;
    } catch (e) {
      return false;
    }
  };
})();
