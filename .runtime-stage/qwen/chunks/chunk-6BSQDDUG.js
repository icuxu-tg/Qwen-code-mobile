// Force strict mode and setup for ESM
"use strict";
import{APPROVAL_MODES}from"./chunk-JZK2WGXA.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function formatApprovalModeError(value){return new Error(`Invalid approval mode: ${value}. Valid values are: ${APPROVAL_MODES.join(", ")}`)}__name(formatApprovalModeError,"formatApprovalModeError");function parseApprovalModeValue(value){const normalized=value.trim().toLowerCase();const canonical=normalized==="auto_edit"||normalized==="autoedit"?"auto-edit":normalized;const approvalMode=APPROVAL_MODES.find(mode=>mode===canonical);if(approvalMode===void 0){throw formatApprovalModeError(value)}return approvalMode}__name(parseApprovalModeValue,"parseApprovalModeValue");export{parseApprovalModeValue};
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
