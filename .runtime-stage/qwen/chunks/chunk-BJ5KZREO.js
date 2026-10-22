// Force strict mode and setup for ESM
"use strict";
import{lookupSkillDisablement}from"./chunk-AVCBF5WL.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();function mapSkillConfigToStatus(skill,disablements=new Map,opts={}){const disablement=lookupSkillDisablement(disablements,skill);const disabledReason=opts.disabled?"inactive_extension":disablement?.reason??(opts.enabled===false?"default":void 0);const modelInvocable=skill.disableModelInvocation!==true;return{kind:"skill",status:disabledReason?"disabled":"ok",name:skill.name,description:skill.description,level:skill.level,modelInvocable,...disabledReason?{disabledReason}:{},...!opts.disabled&&disablement?.lockedScope?{lockedScope:disablement.lockedScope}:{},...skill.userInvocable===false?{userInvocable:false}:{},installedPath:skill.filePath,...skill.argumentHint?{argumentHint:skill.argumentHint}:{},...skill.model?{model:skill.model}:{},...skill.level==="extension"&&skill.extensionName?{extensionName:skill.extensionName}:{},...skill.level==="extension"&&skill.extensionDisplayName?{extensionDisplayName:skill.extensionDisplayName}:{}}}__name(mapSkillConfigToStatus,"mapSkillConfigToStatus");export{mapSkillConfigToStatus};
/**
 * @license
 * Copyright 2025 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
