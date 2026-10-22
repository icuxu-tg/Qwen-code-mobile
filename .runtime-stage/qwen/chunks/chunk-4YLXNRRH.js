// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();import{stripVTControlCharacters}from"node:util";var CONTROL_CHARS_RE=/[\u0000-\u001f\u007f-\u009f]/g;function stripAnsiAndControl(text){return stripVTControlCharacters(text).replace(CONTROL_CHARS_RE,"")}__name(stripAnsiAndControl,"stripAnsiAndControl");function safeLiteralReplace(str,oldString,newString){if(oldString===""||!str.includes(oldString)){return str}if(!newString.includes("$")){return str.replaceAll(oldString,newString)}const escapedNewString=newString.replaceAll("$","$$$$");return str.replaceAll(oldString,escapedNewString)}__name(safeLiteralReplace,"safeLiteralReplace");function isBinary(data,sampleSize=512){if(!data){return false}const sample=data.length>sampleSize?data.subarray(0,sampleSize):data;for(const byte of sample){if(byte===0){return true}}return false}__name(isBinary,"isBinary");function normalizeContent(content){let normalized=content.replace(/^\uFEFF/,"");normalized=normalized.replace(/\r\n/g,"\n").replace(/\r/g,"\n");return normalized}__name(normalizeContent,"normalizeContent");function stripHtmlComments(content){let result=content;let prev;do{prev=result;result=prev.replace(/<!--[\s\S]*?-->/g,"")}while(result!==prev);return result.replace(/<!--/g,"")}__name(stripHtmlComments,"stripHtmlComments");export{stripAnsiAndControl,safeLiteralReplace,isBinary,normalizeContent,stripHtmlComments};
/**
 * @license
 * Copyright 2025 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
