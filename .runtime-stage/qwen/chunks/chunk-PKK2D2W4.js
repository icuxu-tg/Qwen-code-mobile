// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();var PENDING_COMPILE_CACHE_ENV="QWEN_CODE_PENDING_COMPILE_CACHE";function publishPendingCompileCache(env=process.env){const pendingDirectory=env[PENDING_COMPILE_CACHE_ENV];delete env[PENDING_COMPILE_CACHE_ENV];if(pendingDirectory&&!env["NODE_COMPILE_CACHE"]&&env["NODE_DISABLE_COMPILE_CACHE"]!=="1"){env["NODE_COMPILE_CACHE"]=pendingDirectory}}__name(publishPendingCompileCache,"publishPendingCompileCache");init_esbuild_shims();var environmentBeforeLoad;function captureEnvironmentBeforeLoad(){environmentBeforeLoad??=Object.freeze({...process.env})}__name(captureEnvironmentBeforeLoad,"captureEnvironmentBeforeLoad");function getEnvironmentBeforeLoad(){return environmentBeforeLoad}__name(getEnvironmentBeforeLoad,"getEnvironmentBeforeLoad");function resetEnvironmentSnapshotForTesting(){environmentBeforeLoad=void 0}__name(resetEnvironmentSnapshotForTesting,"resetEnvironmentSnapshotForTesting");export{publishPendingCompileCache,captureEnvironmentBeforeLoad,getEnvironmentBeforeLoad,resetEnvironmentSnapshotForTesting};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
