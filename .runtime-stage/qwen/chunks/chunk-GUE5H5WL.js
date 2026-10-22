// Force strict mode and setup for ESM
"use strict";
import{executeBwrap}from"./chunk-6STPSYDE.js";import{executeLandlock}from"./chunk-NWXYAAOL.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function executeSandbox(policy,payload,onOutput,signal,usePty=false,config={},options={}){if(policy.effectiveBackend==="bwrap"){return executeBwrap(policy,payload,onOutput,signal,usePty,config,options)}if(policy.effectiveBackend==="landlock"){return executeLandlock(policy,payload,onOutput,signal,usePty,config,options)}return Promise.reject(new Error("Sandbox backend was not resolved."))}__name(executeSandbox,"executeSandbox");export{executeSandbox};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
