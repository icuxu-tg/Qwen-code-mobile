// Force strict mode and setup for ESM
"use strict";
import{PRIVATE_ACP_CAPABILITY_ENV}from"./chunk-SLD7H2FA.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();var INTERNAL_SECRET_ENV_VARS=["QWEN_SERVER_TOKEN","QWEN_DAEMON_TOKEN","QWEN_CODE_EXTERNAL_TOOL_GUARD_TOKEN",PRIVATE_ACP_CAPABILITY_ENV];var INTERNAL_SECRET_ENV_VAR_NAMES=new Set(INTERNAL_SECRET_ENV_VARS.map(name=>name.toUpperCase()));function isInternalSecretEnvVar(name){return INTERNAL_SECRET_ENV_VAR_NAMES.has(name.toUpperCase())}__name(isInternalSecretEnvVar,"isInternalSecretEnvVar");function sanitizeChildEnv(env=process.env){const sanitized={...env};for(const key of Object.keys(sanitized)){if(isInternalSecretEnvVar(key)){delete sanitized[key]}}return sanitized}__name(sanitizeChildEnv,"sanitizeChildEnv");export{INTERNAL_SECRET_ENV_VARS,isInternalSecretEnvVar,sanitizeChildEnv};
/**
 * @license
 * Copyright 2025 Qwen
 * SPDX-License-Identifier: Apache-2.0
 */
