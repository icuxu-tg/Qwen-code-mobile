// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();import*as path from"node:path";function findCliEntryPath(){const mainModule=process.argv[1];if(mainModule){return path.resolve(mainModule)}throw new Error("Cannot determine CLI entry path")}__name(findCliEntryPath,"findCliEntryPath");export{findCliEntryPath};
