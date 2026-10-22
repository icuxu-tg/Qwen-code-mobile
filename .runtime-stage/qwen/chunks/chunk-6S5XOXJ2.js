// Force strict mode and setup for ESM
"use strict";
import{isLoopbackBind}from"./chunk-3SHHYK7S.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function resolveManagedRuntimeBrokerBaseUrl(raw){let url;try{url=new URL(raw)}catch{throw new Error("Managed Runtime Broker URL is invalid.")}if(url.protocol!=="http:"&&url.protocol!=="https:"||url.username||url.password||url.search||url.hash||url.pathname!=="/"&&url.pathname!==""){throw new Error("Managed Runtime Broker URL must be an HTTP(S) origin.")}if(url.protocol==="http:"&&!isLoopbackBind(url.hostname)){throw new Error("Managed Runtime Broker URL must use HTTPS outside the loopback interface.")}url.pathname="/";return url}__name(resolveManagedRuntimeBrokerBaseUrl,"resolveManagedRuntimeBrokerBaseUrl");export{resolveManagedRuntimeBrokerBaseUrl};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
