// Force strict mode and setup for ESM
"use strict";
import{resolvePath}from"./chunk-YUORVHMI.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();import*as path from"node:path";function isHomeRelative(value){return value==="~"||value.startsWith("~/")||value.startsWith("~\\")}__name(isHomeRelative,"isHomeRelative");function resolveChannelCwd(rawCwd,defaultCwd){if(!rawCwd)return resolvePath(defaultCwd);if(isHomeRelative(rawCwd))return resolvePath(rawCwd);return path.resolve(defaultCwd,rawCwd)}__name(resolveChannelCwd,"resolveChannelCwd");export{resolveChannelCwd};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
