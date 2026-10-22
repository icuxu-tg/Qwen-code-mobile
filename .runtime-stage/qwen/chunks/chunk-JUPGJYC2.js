// Force strict mode and setup for ESM
"use strict";
import{ToolNames}from"./chunk-2UNFJQ2D.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();var EXECUTION_TOOL_NAMES=new Set([ToolNames.READ_FILE,ToolNames.WRITE_FILE,ToolNames.EDIT,ToolNames.NOTEBOOK_EDIT,ToolNames.GLOB,ToolNames.GREP,ToolNames.LS,ToolNames.SHELL,ToolNames.TASK_STOP]);var MANAGED_RUNTIME_TOOL_NAMES=new Set([ToolNames.READ_FILE,ToolNames.WRITE_FILE,ToolNames.EDIT,ToolNames.SHELL]);function stableJson(value){return JSON.stringify(value,(_key,nested)=>nested&&typeof nested==="object"&&!Array.isArray(nested)?Object.fromEntries(Object.entries(nested).sort(([left],[right])=>left<right?-1:left>right?1:0)):nested)}__name(stableJson,"stableJson");var MANAGED_RUNTIME_OUTCOME_UNKNOWN="managed_runtime_outcome_unknown";var ManagedRuntimeOutcomeUnknownError=class extends Error{static{__name(this,"ManagedRuntimeOutcomeUnknownError")}errorKind=MANAGED_RUNTIME_OUTCOME_UNKNOWN;constructor(message,options){super(message,options);this.name="ManagedRuntimeOutcomeUnknownError"}};var ExecutionCleanupError=class extends Error{static{__name(this,"ExecutionCleanupError")}retryCleanup;constructor(message,options){super(message,options);this.retryCleanup=options?.retryCleanup}};export{MANAGED_RUNTIME_TOOL_NAMES,stableJson,MANAGED_RUNTIME_OUTCOME_UNKNOWN,ManagedRuntimeOutcomeUnknownError,ExecutionCleanupError};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
