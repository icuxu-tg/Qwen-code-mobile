// Force strict mode and setup for ESM
"use strict";
import{atomicWriteFileSync}from"./chunk-BMHYEQTI.js";import{createDebugLogger}from"./chunk-JAUJI56S.js";import{Storage}from"./chunk-EPODBRBP.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();import*as fs from"node:fs";import{randomUUID}from"node:crypto";import*as path from"node:path";var debugLogger=createDebugLogger("INSTALLATION");var InstallationManager=class{static{__name(this,"InstallationManager")}getInstallationIdPath(){return Storage.getInstallationIdPath()}readInstallationIdFromFile(){const installationIdFile=this.getInstallationIdPath();if(fs.existsSync(installationIdFile)){const installationid=fs.readFileSync(installationIdFile,"utf-8").trim();return installationid||null}return null}writeInstallationIdToFile(installationId){const installationIdFile=this.getInstallationIdPath();const dir=path.dirname(installationIdFile);fs.mkdirSync(dir,{recursive:true});atomicWriteFileSync(installationIdFile,installationId,{encoding:"utf-8"})}getInstallationId(){try{let installationId=this.readInstallationIdFromFile();if(!installationId){installationId=randomUUID();this.writeInstallationIdToFile(installationId)}return installationId}catch(error){debugLogger.error("Error accessing installation ID file, generating ephemeral ID:",error);return"123456789"}}};export{InstallationManager};
/**
 * @license
 * Copyright 2025 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
