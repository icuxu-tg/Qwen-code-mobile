// packages/core/src/sandbox/bwrap-relay.ts
import { spawn } from "node:child_process";
import {
  closeSync,
  constants,
  fstatSync,
  openSync,
  readFileSync,
  readlinkSync,
  unlinkSync,
  writeFileSync
} from "node:fs";

// packages/core/src/sandbox/sandbox-status.ts
var MAX_STATUS_BYTES = 16 * 1024;

// packages/core/src/sandbox/bwrap-status.ts
function parseBwrapStatus(wire2, exitCode) {
  if (Buffer.byteLength(wire2) > MAX_STATUS_BYTES || !wire2.endsWith("\n")) {
    return { state: "unconfirmed" };
  }
  if (wire2 === '{"state":"stdio-setup-failed"}\n' && exitCode === 125)
    return { state: "unconfirmed", payloadExitObserved: false };
  const payloadExitObserved = wire2.trim().split("\n").some((line) => {
    try {
      const record = JSON.parse(line);
      const code = record["exit-code"];
      return typeof code === "number" && Number.isInteger(code) && code >= 0 && code <= 255;
    } catch {
      return false;
    }
  });
  if (wire2.trim().split("\n").some((line) => {
    try {
      return JSON.parse(line)?.["state"] === "stdio-failed";
    } catch {
      return false;
    }
  })) {
    return payloadExitObserved ? { state: "unconfirmed", payloadExitObserved: true } : { state: "unconfirmed" };
  }
  try {
    const lines = wire2.trim().split("\n");
    if (lines.length !== 2)
      return { state: "unconfirmed", payloadExitObserved };
    const initial = JSON.parse(lines[0]);
    const final = JSON.parse(lines[1]);
    const pid = initial["child-pid"];
    const code = final["exit-code"];
    if (typeof pid === "number" && Number.isInteger(pid) && pid > 0 && typeof code === "number" && Number.isInteger(code) && code >= 0 && code <= 255 && code === exitCode) {
      return { state: "confirmed", exitCode: code };
    }
  } catch {
  }
  return { state: "unconfirmed", payloadExitObserved };
}

// packages/core/src/sandbox/bwrap-relay.ts
var [parentPid, statusPath, payloadEnvPath, inputBridge, bwrap, ...args] = process.argv.slice(2);
if (process.ppid !== Number(parentPid)) process.exit(1);
var parentWatch = setInterval(() => {
  if (process.ppid !== Number(parentPid)) process.exit(1);
}, 100);
parentWatch.unref();
var fd = openSync(
  statusPath,
  constants.O_WRONLY | constants.O_CREAT | constants.O_EXCL | constants.O_NOFOLLOW,
  384
);
var envFd = openSync(
  payloadEnvPath,
  constants.O_RDONLY | constants.O_NOFOLLOW
);
var parsedEnv;
try {
  parsedEnv = JSON.parse(readFileSync(envFd, "utf8"));
} finally {
  closeSync(envFd);
  unlinkSync(payloadEnvPath);
}
if (!parsedEnv || typeof parsedEnv !== "object" || Array.isArray(parsedEnv))
  process.exit(1);
var env = Object.fromEntries(
  Object.entries(parsedEnv).map(([key, value]) => {
    if (typeof value !== "string") process.exit(1);
    return [key, value];
  })
);
var input = fstatSync(0);
var shareInput = input.isSocket() || input.isCharacterDevice();
if (input.isFIFO()) {
  try {
    shareInput = readlinkSync("/proc/self/fd/0").startsWith("pipe:");
  } catch {
    shareInput = false;
  }
}
if (!shareInput && !inputBridge) {
  writeFileSync(
    fd,
    JSON.stringify({ state: "unconfirmed", payloadExitObserved: false })
  );
  closeSync(fd);
  process.stderr.write(
    "Host-backed stdin requires the Linux x64/arm64 input helper.\n"
  );
  process.exit(125);
}
void process.stdout;
void process.stderr;
var backendArgs = ["--json-status-fd", "3", ...args];
var child = spawn(
  shareInput ? bwrap : inputBridge,
  shareInput ? backendArgs : ["--relay-stdin", bwrap, ...backendArgs],
  {
    stdio: ["inherit", "inherit", "inherit", "pipe"],
    env
  }
);
var wire = "";
var bytes = 0;
var failed = false;
var spawnFailed = false;
var statusStream = child.stdio[3];
statusStream.on("data", (chunk) => {
  bytes += chunk.length;
  if (bytes <= MAX_STATUS_BYTES) wire += chunk.toString("utf8");
  else failed = true;
});
statusStream.on("error", () => {
  failed = true;
});
child.on("error", () => {
  failed = true;
  spawnFailed = true;
});
child.on("close", (code, signal) => {
  clearInterval(parentWatch);
  const status = signal ? { state: "interrupted" } : failed ? spawnFailed ? { state: "unconfirmed", payloadExitObserved: false } : { state: "unconfirmed" } : parseBwrapStatus(wire, code);
  writeFileSync(fd, JSON.stringify(status));
  closeSync(fd);
  if (signal) process.kill(process.pid, signal);
  else process.exitCode = code ?? 1;
});
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
