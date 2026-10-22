// Force strict mode and setup for ESM
"use strict";
import{sandboxAsset}from"./chunk-7C6FWKCH.js";import{Storage}from"./chunk-EPODBRBP.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();import path from"node:path";function createExecutionSandboxPolicy(settings,workspace){if(process.platform!=="linux"){throw new Error("tools.executionSandbox requires Linux with a compatible bwrap or Landlock backend.")}return{requestedBackend:settings.backend??"auto",filesystem:settings.filesystem,network:settings.network,workspace:path.resolve(workspace),installation:path.dirname(sandboxAsset("bwrap-relay")),state:path.join(Storage.getRuntimeBaseDir(),"sandbox")}}__name(createExecutionSandboxPolicy,"createExecutionSandboxPolicy");export{createExecutionSandboxPolicy};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
