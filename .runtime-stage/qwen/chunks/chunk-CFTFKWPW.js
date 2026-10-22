// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();var REDACTED="<redacted>";var CREDENTIAL_PATTERNS=[{pattern:/(Bearer\s+)[A-Za-z0-9._~+/=-]+/gi,replacement:`$1${REDACTED}`},{pattern:/(QQBot\s+)[A-Za-z0-9._~+/=-]+/gi,replacement:`$1${REDACTED}`},{pattern:/(Authorization:\s*)\S+(?:\s+\S+)?/gi,replacement:`$1${REDACTED}`},{pattern:/(x-acs-dingtalk-access-token:\s*)\S+/gi,replacement:`$1${REDACTED}`},{pattern:/sk-[a-zA-Z0-9-]{20,}/g,replacement:`sk-${REDACTED}`},{pattern:/(?:ghp_|gho_|ghs_|ghu_|github_pat_|glpat-|xox[b]-|xox[p]-)[a-zA-Z0-9_-]{20,}/g,replacement:REDACTED},{pattern:/(?:AKIA|ASIA)[A-Z0-9]{16}/g,replacement:REDACTED},{pattern:/((?:api[_-]?key|token|secret|password|pwd)[_-]?[=:]\s*)\S{10,}/gi,replacement:`$1${REDACTED}`},{pattern:/([A-Z][A-Z0-9]{0,50}(?:_[A-Z0-9]{1,50}){0,10}_(?:KEY|TOKEN|SECRET|PASSWORD)\s*[=:]\s*)\S{10,}/g,replacement:`$1${REDACTED}`},{pattern:/("(?:api_key|api-key|apikey|token|secret|password|pwd|access_token|client_secret|app_secret|authorization)"\s*:\s*")[^"]{10,}(")/gi,replacement:`$1${REDACTED}$2`},{pattern:/\b([a-z][a-z0-9+.-]{0,31}:\/\/)(?:[^/\s]+@)+/gi,replacement:`$1${REDACTED}@`}];function redactLogCredentials(line){let result=line;for(const{pattern,replacement}of CREDENTIAL_PATTERNS){result=result.replace(pattern,replacement)}return result}__name(redactLogCredentials,"redactLogCredentials");export{redactLogCredentials};
/**
 * @license
 * Copyright 2025 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
