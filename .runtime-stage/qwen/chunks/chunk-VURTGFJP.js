// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function flushOutput(stream,signal){if(signal.aborted)return Promise.resolve();return new Promise((resolve,reject)=>{const cancel=__name(()=>resolve(),"cancel");signal.addEventListener("abort",cancel,{once:true});stream.write(Buffer.alloc(0),error=>{signal.removeEventListener("abort",cancel);if(error){setImmediate(()=>signal.aborted?resolve():reject(error))}else resolve()})})}__name(flushOutput,"flushOutput");export{flushOutput};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
