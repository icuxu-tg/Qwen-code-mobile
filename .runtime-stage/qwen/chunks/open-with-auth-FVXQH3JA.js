// Force strict mode and setup for ESM
"use strict";
import{resolveServeToken}from"./chunk-RZMDX7FA.js";import{resolveWebShellDir}from"./chunk-N6DRGTL5.js";import"./chunk-P4Y2SYO4.js";import{isLoopbackBind}from"./chunk-3SHHYK7S.js";import{writeStderrLine}from"./chunk-SLPJ4OII.js";import"./chunk-4YLXNRRH.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();import{randomBytes}from"node:crypto";function applyOpenWithAuth(options){if(!isLoopbackBind(options.hostname)){throw new Error("--open-with-auth requires a loopback --hostname.")}if(options.serveWebShell===false){throw new Error("--open-with-auth requires the Web Shell; omit --no-web.")}if(!resolveWebShellDir()){throw new Error("--open-with-auth requires built Web Shell assets.")}options.requireWebShell=true;const configuredToken=resolveServeToken(options.token);if(configuredToken){options.token=configuredToken;return}options.token=randomBytes(32).toString("base64url");writeStderrLine("qwen serve: temporary bearer authentication enabled for this Web Shell launch; use an explicit shared token for additional clients.")}__name(applyOpenWithAuth,"applyOpenWithAuth");export{applyOpenWithAuth};
/**
 * @license
 * Copyright 2025 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
