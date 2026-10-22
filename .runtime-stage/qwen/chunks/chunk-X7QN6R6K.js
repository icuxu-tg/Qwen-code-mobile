// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function normalizeServeFastPathArgv(rawArgv){const argv=[...rawArgv];const firstArg=argv[0]?.replace(/\\/g,"/");if(firstArg!==void 0&&(firstArg.endsWith("/dist/qwen-cli/cli.js")||firstArg.endsWith("/dist/cli.js")||firstArg.endsWith("/dist/cli/cli.js"))){return argv.slice(1)}return argv}__name(normalizeServeFastPathArgv,"normalizeServeFastPathArgv");function isServeFastPathArgv(rawArgv){return normalizeServeFastPathArgv(rawArgv)[0]==="serve"}__name(isServeFastPathArgv,"isServeFastPathArgv");export{normalizeServeFastPathArgv,isServeFastPathArgv};
/**
 * @license
 * Copyright 2025 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
