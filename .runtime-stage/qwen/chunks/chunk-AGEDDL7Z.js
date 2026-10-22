// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();import{AsyncLocalStorage}from"node:async_hooks";var teammateIdentityStore=new AsyncLocalStorage;function getTeammateContext(){return teammateIdentityStore.getStore()}__name(getTeammateContext,"getTeammateContext");function isInProcessTeammate(){return teammateIdentityStore.getStore()!==void 0}__name(isInProcessTeammate,"isInProcessTeammate");function getAgentName(){return teammateIdentityStore.getStore()?.agentName}__name(getAgentName,"getAgentName");function getTeamName(){return teammateIdentityStore.getStore()?.teamName}__name(getTeamName,"getTeamName");function resolveActiveTeamName(fallback){return getTeamName()??fallback}__name(resolveActiveTeamName,"resolveActiveTeamName");var isTeammate=isInProcessTeammate;function runWithTeammateIdentity(identity,fn){return teammateIdentityStore.run(identity,fn)}__name(runWithTeammateIdentity,"runWithTeammateIdentity");function runOutsideTeammateIdentity(fn){return teammateIdentityStore.exit(fn)}__name(runOutsideTeammateIdentity,"runOutsideTeammateIdentity");export{getTeammateContext,getAgentName,getTeamName,resolveActiveTeamName,isTeammate,runWithTeammateIdentity,runOutsideTeammateIdentity};
/**
 * @license
 * Copyright 2025 Qwen
 * SPDX-License-Identifier: Apache-2.0
 */
