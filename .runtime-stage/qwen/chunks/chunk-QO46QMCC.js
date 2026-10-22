// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function hasRootlessMarker(info){try{const parsed=JSON.parse(info);return Array.isArray(parsed?.SecurityOptions)&&parsed.SecurityOptions.includes("name=rootless")||parsed?.Host?.Security?.Rootless===true||parsed?.host?.security?.rootless===true}catch{return false}}__name(hasRootlessMarker,"hasRootlessMarker");export{hasRootlessMarker};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
