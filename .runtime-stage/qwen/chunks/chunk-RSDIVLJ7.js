// Force strict mode and setup for ESM
"use strict";
import{WEB_SHELL_PWA_ASSETS}from"./chunk-6DBL5YKZ.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function isDocumentNavigation(req){const fetchMode=req.headers["sec-fetch-mode"];const fetchDest=req.headers["sec-fetch-dest"];const accept=req.headers.accept??"";return fetchMode==="navigate"||fetchDest==="document"||accept.trim().toLowerCase().startsWith("text/html")}__name(isDocumentNavigation,"isDocumentNavigation");var WEB_SHELL_PAGE_PATHS=["/plugins","/channels","/live","/scheduled-tasks","/goals","/settings"];var SESSION_DEEP_LINK_PATH=/^\/session\/[^/]+\/?$/u;function isPreAuthWebShellRequest(req){if(req.method!=="GET"&&req.method!=="HEAD")return false;const reqPath=req.path.toLowerCase();if(reqPath==="/"||reqPath==="//"||reqPath==="/assets"||reqPath.startsWith("/assets/")||reqPath==="/mcp-app-sandbox"||WEB_SHELL_PWA_ASSETS.some(({route})=>reqPath===route||reqPath===`${route}/`))return true;return(SESSION_DEEP_LINK_PATH.test(reqPath)||WEB_SHELL_PAGE_PATHS.some(route=>reqPath===route||reqPath===`${route}/`))&&isDocumentNavigation(req)}__name(isPreAuthWebShellRequest,"isPreAuthWebShellRequest");export{isDocumentNavigation,WEB_SHELL_PAGE_PATHS,isPreAuthWebShellRequest};
/**
 * @license
 * Copyright 2025 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
