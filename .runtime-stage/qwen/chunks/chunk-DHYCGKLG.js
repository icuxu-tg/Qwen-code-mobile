// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();import{createHash}from"node:crypto";var AGENT_HOST_SESSION_SOURCE_TYPE="agent-host";var AGENT_SESSION_SOURCE_TYPE="agent";var HIDDEN_CATALOG_SOURCE_TYPES=[AGENT_HOST_SESSION_SOURCE_TYPE,AGENT_SESSION_SOURCE_TYPE];function isHiddenCatalogSource(sourceType){return sourceType!==void 0&&HIDDEN_CATALOG_SOURCE_TYPES.includes(sourceType)}__name(isHiddenCatalogSource,"isHiddenCatalogSource");function uuidV5Url(name){const bytes=createHash("sha1").update(Buffer.from("6ba7b8119dad11d180b400c04fd430c8","hex")).update(name).digest().subarray(0,16);bytes[6]=bytes[6]&15|80;bytes[8]=bytes[8]&63|128;const hex=bytes.toString("hex");return`${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`}__name(uuidV5Url,"uuidV5Url");function sessionAgentNativeSessionId(agentId,chatSessionId){return uuidV5Url(`qwen-code:session-agent:${agentId}:chat-session:${chatSessionId}`)}__name(sessionAgentNativeSessionId,"sessionAgentNativeSessionId");export{AGENT_HOST_SESSION_SOURCE_TYPE,AGENT_SESSION_SOURCE_TYPE,HIDDEN_CATALOG_SOURCE_TYPES,isHiddenCatalogSource,sessionAgentNativeSessionId};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
