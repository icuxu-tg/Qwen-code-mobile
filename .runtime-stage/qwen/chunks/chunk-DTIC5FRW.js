// Force strict mode and setup for ESM
"use strict";
import{SessionService}from"./chunk-X5CTAVPR.js";import{Storage}from"./chunk-EPODBRBP.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function runWithWorkspaceRuntimeStorage(runtime,fn){return Storage.runWithResolvedRuntimeBaseDir(runtime.sessionRuntimeBaseDir,fn)}__name(runWithWorkspaceRuntimeStorage,"runWithWorkspaceRuntimeStorage");function createWorkspaceRuntimeSessionService(runtime,options={}){return new SessionService(runtime.workspaceCwd,{...options,runtimeBaseDir:runtime.sessionRuntimeBaseDir})}__name(createWorkspaceRuntimeSessionService,"createWorkspaceRuntimeSessionService");export{runWithWorkspaceRuntimeStorage,createWorkspaceRuntimeSessionService};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
