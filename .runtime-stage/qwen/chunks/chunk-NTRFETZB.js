// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();var BYTES_PER_KB=1024;var BYTES_PER_MB=BYTES_PER_KB*1024;var BYTES_PER_GB=BYTES_PER_MB*1024;var roundedTo=__name((value,digits)=>Number(value.toFixed(digits)),"roundedTo");var formatMemoryUsage=__name(bytes=>{if(roundedTo(bytes/BYTES_PER_KB,1)<1024){return`${(bytes/BYTES_PER_KB).toFixed(1)} KB`}if(roundedTo(bytes/BYTES_PER_MB,1)<1024){return`${(bytes/BYTES_PER_MB).toFixed(1)} MB`}return`${(bytes/BYTES_PER_GB).toFixed(2)} GB`},"formatMemoryUsage");var JSON_TAG_CHARACTER_ESCAPES={"<":"\\u003c",">":"\\u003e","&":"\\u0026"};var escapeJsonTagCharacters=__name(json=>json.replace(/[<>&]/g,character=>JSON_TAG_CHARACTER_ESCAPES[character]),"escapeJsonTagCharacters");export{formatMemoryUsage,escapeJsonTagCharacters};
/**
 * @license
 * Copyright 2025 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
