// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();import{fileURLToPath}from"node:url";import path from"node:path";var __filename=fileURLToPath(import.meta.url);var __dirname=path.dirname(__filename);var packageJson;async function getPackageJson(){if(packageJson){return packageJson}const{readPackageUp}=await import("./read-package-up-ZCXJGON6.js");const result=await readPackageUp({cwd:__dirname});if(!result){return}packageJson=result.packageJson;return packageJson}__name(getPackageJson,"getPackageJson");export{getPackageJson};
/**
 * @license
 * Copyright 2025 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
