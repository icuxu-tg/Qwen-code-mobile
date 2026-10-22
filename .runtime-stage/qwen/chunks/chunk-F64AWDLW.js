// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function fireSessionDeleteHook(config,sessionId,logger=config.getDebugLogger()){void config.getHookSystem()?.fireSessionDeleteEvent(sessionId).catch(error=>{logger.warn(`SessionDelete hook failed for ${sessionId}: ${error instanceof Error?error.message:String(error)}`)})}__name(fireSessionDeleteHook,"fireSessionDeleteHook");export{fireSessionDeleteHook};
/**
 * @license
 * Copyright 2025 Qwen Code
 * SPDX-License-Identifier: Apache-2.0
 */
