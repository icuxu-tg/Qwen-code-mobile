// Force strict mode and setup for ESM
"use strict";
import{getEffectiveOmniUploadConfig}from"./chunk-MDCD37JO.js";import{createDebugLogger}from"./chunk-JAUJI56S.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();var debugLogger=createDebugLogger("omni:delivery-gate");function isOmniDeliveryActive(config){if(!config.isOmniEnabled?.())return false;if(config.isTrustedFolder?.()===false){debugLogger.debug("omni delivery inactive: untrusted workspace");return false}if(!getEffectiveOmniUploadConfig(config)){debugLogger.debug("omni delivery inactive: no usable DashScope upload configuration");return false}return true}__name(isOmniDeliveryActive,"isOmniDeliveryActive");export{isOmniDeliveryActive};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
