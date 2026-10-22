// Force strict mode and setup for ESM
"use strict";
import{init_esbuild_shims}from"./chunks/chunk-VKDAXTCZ.js";import"./chunks/chunk-KBWJGX7I.js";init_esbuild_shims();import{Console}from"node:console";globalThis.console=new Console(process.stderr,process.stderr);try{const options=JSON.parse(process.argv[2]??"");process.chdir(options.workspace);const{createExecutionWorkerEnvironment,runExecutionWorker}=await import("./chunks/execution-worker-UYNEBM56.js");await runExecutionWorker(createExecutionWorkerEnvironment(options),process.stdin,process.stdout);process.exit(0)}catch(error){process.stderr.write(`${error instanceof Error?error.message:String(error)}
`);process.exit(1)}
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
