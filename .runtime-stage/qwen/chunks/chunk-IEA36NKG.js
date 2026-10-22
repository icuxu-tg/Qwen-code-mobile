// Force strict mode and setup for ESM
"use strict";
import{getPackageJson}from"./chunk-O4EDHV64.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();async function getCliVersion(){const pkgJson=await getPackageJson();return"0.25.3"}__name(getCliVersion,"getCliVersion");function formatVersionLabel(version){return/^\d/.test(version)?`v${version}`:version}__name(formatVersionLabel,"formatVersionLabel");export{getCliVersion,formatVersionLabel};
/**
 * @license
 * Copyright 2025 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
