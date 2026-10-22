// Force strict mode and setup for ESM
"use strict";
import{SessionOrganizationService}from"./chunk-X5CTAVPR.js";import{writeStderrLine}from"./chunk-SLPJ4OII.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function createSessionOrganizationService(workspaceCwd){return new SessionOrganizationService(workspaceCwd,message=>{writeStderrLine(`qwen serve: session-org: ${message}`)})}__name(createSessionOrganizationService,"createSessionOrganizationService");export{createSessionOrganizationService};
/**
 * @license
 * Copyright 2025 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
