// Force strict mode and setup for ESM
"use strict";
import{PRIVATE_RELAUNCH_ENV_PROVENANCE}from"./chunk-CVMMNPLX.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();var CONTAINER_HOME="/qwen-review-home";function containerEnv(cacheDir){return["CI=1","npm_config_yes=true","QWEN_SKIP_PREPARE=1",`HOME=${CONTAINER_HOME}`,`npm_config_cache=${cacheDir}`]}__name(containerEnv,"containerEnv");function trustedProcessEnv(env,isFileSourcedEnvKey){const scrubbed={...env};for(const key of Object.keys(scrubbed)){if(key.toLowerCase()===PRIVATE_RELAUNCH_ENV_PROVENANCE.toLowerCase()||isFileSourcedEnvKey(key))delete scrubbed[key]}return scrubbed}__name(trustedProcessEnv,"trustedProcessEnv");export{CONTAINER_HOME,containerEnv,trustedProcessEnv};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
