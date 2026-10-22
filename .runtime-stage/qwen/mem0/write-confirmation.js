import { createRequire } from 'node:module'; const require = createRequire(import.meta.url);

// integrations/external-context/src/write-confirmation.ts
import { realpath } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

// integrations/external-context/src/memory-content.ts
var MAX_MEMORY_CONTENT_CHARACTERS = 4e3;
var NON_CONTENT_CHARACTER = /^[\p{White_Space}\p{Cc}\p{Cf}]$/u;
var DISPLAY_ESCAPE_CHARACTER = /[\u007f-\u009f\u2028\u2029\p{Cf}]/gu;
function isValidMemoryContent(value) {
  if (Array.from(value).length > MAX_MEMORY_CONTENT_CHARACTERS || hasUnpairedSurrogate(value)) {
    return false;
  }
  return Array.from(value).some(
    (character) => !NON_CONTENT_CHARACTER.test(character)
  );
}
function renderMemoryContentForConfirmation(value) {
  return JSON.stringify(value).replace(
    DISPLAY_ESCAPE_CHARACTER,
    (character) => character.split("").map(
      (codeUnit) => `\\u${codeUnit.charCodeAt(0).toString(16).padStart(4, "0")}`
    ).join("")
  );
}
function hasUnpairedSurrogate(value) {
  for (let index = 0; index < value.length; index += 1) {
    const code = value.charCodeAt(index);
    if (code >= 55296 && code <= 56319) {
      if (index + 1 >= value.length) {
        return true;
      }
      const next = value.charCodeAt(index + 1);
      if (next < 56320 || next > 57343) {
        return true;
      }
      index += 1;
    } else if (code >= 56320 && code <= 57343) {
      return true;
    }
  }
  return false;
}

// integrations/external-context/src/write-confirmation.ts
var MAX_HOOK_INPUT_BYTES = 1024 * 1024;
var REMEMBER_TOOL_NAME = "mcp__external-context__context_remember";
var ASK_PERMISSION_MODES = /* @__PURE__ */ new Set([
  "default",
  "auto",
  "auto_edit",
  "auto-edit",
  "yolo"
]);
function runWriteConfirmation(value) {
  if (!isRecord(value)) {
    return denyInvalid();
  }
  if (value["hook_event_name"] !== "PreToolUse" || value["tool_name"] !== REMEMBER_TOOL_NAME) {
    return {};
  }
  const toolInput = value["tool_input"];
  const permissionMode = value["permission_mode"];
  if (typeof permissionMode !== "string" || !isRecord(toolInput) || typeof toolInput["content"] !== "string" || !isValidMemoryContent(toolInput["content"])) {
    return denyInvalid();
  }
  if (permissionMode === "plan") {
    return deny("External context memory writes are not allowed in plan mode.");
  }
  if (!ASK_PERMISSION_MODES.has(permissionMode)) {
    return deny(
      "External context memory write permission mode is unsupported."
    );
  }
  return {
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "ask",
      permissionDecisionReason: "Save this exact content to the bound Mem0 repository memory?\n" + renderMemoryContentForConfirmation(toolInput["content"])
    }
  };
}
async function runWriteConfirmationCli(inputStream = process.stdin, outputStream = process.stdout) {
  const input = await readHookInput(inputStream).catch(() => void 0);
  outputStream.write(JSON.stringify(runWriteConfirmation(input)));
}
function denyInvalid() {
  return deny("External context memory write confirmation request is invalid.");
}
function deny(reason) {
  return {
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: reason
    }
  };
}
async function readHookInput(inputStream) {
  const chunks = [];
  let total = 0;
  for await (const source of inputStream) {
    const chunk = Buffer.from(source);
    total += chunk.byteLength;
    if (total > MAX_HOOK_INPUT_BYTES) {
      inputStream.destroy?.();
      return void 0;
    }
    chunks.push(chunk);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString("utf8"));
  } catch {
    return void 0;
  }
}
function isRecord(value) {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
async function isDirectEntryPoint() {
  const entryPoint = process.argv[1];
  if (entryPoint === void 0) {
    return false;
  }
  try {
    return await realpath(fileURLToPath(import.meta.url)) === await realpath(resolve(entryPoint));
  } catch {
    return false;
  }
}
if (await isDirectEntryPoint()) {
  try {
    await runWriteConfirmationCli();
  } catch {
    process.exitCode = 1;
  }
}
export {
  runWriteConfirmation,
  runWriteConfirmationCli
};
/**
 * @license
 * Copyright 2025 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
