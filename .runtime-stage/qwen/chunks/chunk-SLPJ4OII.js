// Force strict mode and setup for ESM
"use strict";
import{stripAnsiAndControl}from"./chunk-4YLXNRRH.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();var LOG_LINE_UNSAFE_RE=/[\x00-\x1f\x7f-\x9f\u200b-\u200f\u2028-\u202e\u2066-\u2069\ufeff]/g;function sanitizeDaemonLogLine(text){return stripAnsiAndControl(text).replace(LOG_LINE_UNSAFE_RE," ").slice(0,4096)}__name(sanitizeDaemonLogLine,"sanitizeDaemonLogLine");var writeStdoutLine=__name(message=>{process.stdout.write(message.endsWith("\n")?message:`${message}
`)},"writeStdoutLine");var writeStderrLine=__name(message=>{process.stderr.write(message.endsWith("\n")?message:`${message}
`)},"writeStderrLine");var writeStdoutLineSafe=__name(message=>{try{writeStdoutLine(message)}catch{}},"writeStdoutLineSafe");var writeStderrLineSafe=__name(message=>{try{writeStderrLine(message)}catch{}},"writeStderrLineSafe");var clearScreen=__name(()=>{console.clear()},"clearScreen");var ignoreBrokenPipe=__name(()=>{process.stdout.on("error",err=>{if(err.code==="EPIPE")process.stdout.destroy()});process.stderr.on("error",err=>{if(err.code==="EPIPE")process.stderr.destroy()})},"ignoreBrokenPipe");export{LOG_LINE_UNSAFE_RE,sanitizeDaemonLogLine,writeStdoutLine,writeStderrLine,writeStdoutLineSafe,writeStderrLineSafe,clearScreen,ignoreBrokenPipe};
/**
 * @license
 * Copyright 2025 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
