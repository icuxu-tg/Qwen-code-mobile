// Force strict mode and setup for ESM
"use strict";
import{getErrorMessage}from"./chunk-4YQ7IW7O.js";import{extensionToOutputString,getExtensionManager}from"./chunk-HEBWULMY.js";import{loadSettings}from"./chunk-IRXTNVRS.js";import{initializeI18n,resolveLanguageSetting,t}from"./chunk-V4NQX7F4.js";import{writeStderrLine,writeStdoutLine}from"./chunk-SLPJ4OII.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();async function handleList(){try{const settings=loadSettings();await initializeI18n(resolveLanguageSetting(settings.merged.general?.language));const extensionManager=await getExtensionManager();const extensions=extensionManager.getLoadedExtensions();if(!extensions||extensions.length===0){writeStdoutLine(t("No extensions installed."));return}writeStdoutLine(extensions.map((extension,_)=>extensionToOutputString(extension,extensionManager,process.cwd())).join("\n\n"))}catch(error){writeStderrLine(getErrorMessage(error));process.exit(1)}}__name(handleList,"handleList");var listCommand={command:"list",describe:t("Lists installed extensions."),builder:__name(yargs=>yargs,"builder"),handler:__name(async()=>{await handleList()},"handler")};export{handleList,listCommand};
/**
 * @license
 * Copyright 2025 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
