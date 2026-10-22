// Force strict mode and setup for ESM
"use strict";
import{InProcessBackend,TeamManager}from"./chunk-X5CTAVPR.js";import"./chunk-R2EHHA2C.js";import"./chunk-ZLZF2ZDD.js";import"./chunk-JUPGJYC2.js";import"./chunk-YK7NDQKM.js";import"./chunk-U7JDDQN6.js";import"./chunk-OGI5HG6N.js";import"./chunk-T7CHK6C2.js";import"./chunk-VZDZ34KK.js";import"./chunk-PT7NZBDY.js";import"./chunk-TWP72VRH.js";import"./chunk-4COB4AZT.js";import"./chunk-PHIPLV2I.js";import{clearAllInboxes}from"./chunk-WK2IBT6D.js";import{resetTaskList}from"./chunk-UYEWRLGD.js";import"./chunk-LLX4BCWK.js";import"./chunk-QXQGUM3Y.js";import"./chunk-AG3SSCAF.js";import"./chunk-BMSATZXW.js";import"./chunk-57RLZJ6C.js";import"./chunk-EOMQENJY.js";import"./chunk-QVPT37XZ.js";import"./chunk-VMQO3VPA.js";import"./chunk-HATHFJPW.js";import"./chunk-XQJOZDV7.js";import"./chunk-CV2UAEZA.js";import"./chunk-GNQGC2DJ.js";import"./chunk-YPIUTOL3.js";import"./chunk-KLC3OD2M.js";import"./chunk-K6EQXZMK.js";import"./chunk-BG67XABA.js";import"./chunk-BEKROCWN.js";import"./chunk-BYU42MLK.js";import{LEADER_NAME,MAX_TEAMMATES,createTeamFile,formatAgentId,getTasksDir,getTeamDir,sanitizeName,tryReclaimStaleTeam}from"./chunk-JWQXU3SR.js";import"./chunk-5LZ2DPLH.js";import"./chunk-JVJS2YFU.js";import"./chunk-G5PYYZMM.js";import"./chunk-D7XCQWP2.js";import"./chunk-OBOU4ZZM.js";import"./chunk-D6UTJ3SP.js";import"./chunk-MPHSE7M4.js";import"./chunk-FQQMMFT6.js";import"./chunk-JZK2WGXA.js";import"./chunk-GUE5H5WL.js";import"./chunk-BSMIT3UQ.js";import"./chunk-JKXBHBE3.js";import"./chunk-CLOI3KNX.js";import"./chunk-MDCD37JO.js";import"./chunk-S2ZKZXHX.js";import"./chunk-PXXU2KWH.js";import"./chunk-UGFTA2VG.js";import"./chunk-EBMCWXGZ.js";import"./chunk-6STPSYDE.js";import"./chunk-NWXYAAOL.js";import"./chunk-4ZRCV3JY.js";import"./chunk-7C6FWKCH.js";import"./chunk-YGLM5XPP.js";import"./chunk-WGNF7FPC.js";import"./chunk-4NPYOXJO.js";import"./chunk-MWGB5RCF.js";import"./chunk-XWEXQT3X.js";import"./chunk-NTRFETZB.js";import"./chunk-TL3SF2GA.js";import"./chunk-YIW3H46P.js";import"./chunk-7UK4WJWS.js";import"./chunk-4GP3PH7Z.js";import"./chunk-LFKUPU57.js";import"./chunk-T2F7KYYR.js";import"./chunk-SW54YEPK.js";import"./chunk-E2IJS24D.js";import"./chunk-7J2TOJMG.js";import"./chunk-AGEDDL7Z.js";import"./chunk-2NJJHZ52.js";import"./chunk-3YWXKSOI.js";import"./chunk-OW25BC3M.js";import"./chunk-2PMQL4KE.js";import"./chunk-V3XYE727.js";import"./chunk-S6RXVOY6.js";import"./chunk-BRPC43OM.js";import"./chunk-CRWD3V2H.js";import"./chunk-WZDBBDRD.js";import"./chunk-476F6QIM.js";import"./chunk-4R67LXKL.js";import"./chunk-WW7MJFAN.js";import"./chunk-KBPIYGHG.js";import"./chunk-UQFTXKEB.js";import"./chunk-HDGTWJ24.js";import"./chunk-W5NWIGKO.js";import"./chunk-JU2HHQR5.js";import"./chunk-6C4YRBLR.js";import"./chunk-37KFLBKZ.js";import"./chunk-PWOQASXT.js";import"./chunk-Z6AR5XE5.js";import"./chunk-7U7CWMP7.js";import"./chunk-77V43YOE.js";import"./chunk-SLD7H2FA.js";import"./chunk-7DOOXUDG.js";import"./chunk-P3RLMP7L.js";import"./chunk-R54OXALA.js";import"./chunk-GPWJSDEY.js";import"./chunk-TAAJ7TDB.js";import"./chunk-4ZBZL23U.js";import"./chunk-ZZZAYAJW.js";import"./chunk-XMANRGPK.js";import"./chunk-AWTUIQ7W.js";import"./chunk-KQX4H2D3.js";import"./chunk-BWU6AH5O.js";import"./chunk-BMHYEQTI.js";import"./chunk-CPVAVCGD.js";import"./chunk-4YLXNRRH.js";import"./chunk-65UCR6OZ.js";import"./chunk-CN2TQ3PU.js";import"./chunk-6GTPLW7K.js";import"./chunk-NGVRCUYD.js";import"./chunk-5DTFN37U.js";import"./chunk-2YUBAWQ6.js";import{BaseDeclarativeTool,BaseToolInvocation}from"./chunk-O5EKX7LG.js";import"./chunk-TM6ZCBR7.js";import"./chunk-I6S7NGX3.js";import"./chunk-SVAPAMMG.js";import"./chunk-3KUHQGOS.js";import{ToolDisplayNames,ToolNames}from"./chunk-2UNFJQ2D.js";import"./chunk-JAUJI56S.js";import"./chunk-I4H7NUVP.js";import"./chunk-EPODBRBP.js";import"./chunk-OKN3Z5C5.js";import{isNodeError}from"./chunk-XYB7GC5U.js";import"./chunk-2UOMCYQG.js";import"./chunk-I7L2TX4M.js";import"./chunk-MK3OONRA.js";import"./chunk-COGTQ5KZ.js";import{init_esbuild_shims}from"./chunk-VKDAXTCZ.js";import{__name}from"./chunk-KBWJGX7I.js";init_esbuild_shims();var TeamCreateInvocation=class extends BaseToolInvocation{constructor(config,params){super(params);this.config=config}static{__name(this,"TeamCreateInvocation")}getDescription(){return`Create team "${this.params.team_name}"`}async execute(){const teamName=sanitizeName(this.params.team_name);if(!teamName){const msg="Team name is required.";return{llmContent:msg,returnDisplay:msg,error:{message:msg}}}if(this.config.getArenaManager()){const msg="Cannot create a team while an Arena session is active. End the Arena session first.";return{llmContent:msg,returnDisplay:msg,error:{message:msg}}}if(this.config.getTeamManager()){const msg="A team is already active. Delete it before creating a new one.";return{llmContent:msg,returnDisplay:msg,error:{message:msg}}}if(this.config.getAgentExecutionBackend?.()==="container"){const msg="Container execution is required; team and Arena agents are unsupported.";return{llmContent:msg,returnDisplay:msg,error:{message:msg}}}const leadAgentId=formatAgentId(LEADER_NAME,teamName);const teamFile={name:teamName,description:this.params.description,createdAt:Date.now(),leadAgentId,leadSessionId:this.config.getSessionId(),leadPid:process.pid,members:[]};try{await createTeamFile(teamName,teamFile)}catch(err){if(isNodeError(err)&&err.code==="EEXIST"){const reclaimed=await tryReclaimStaleTeam(teamName);if(!reclaimed){const msg=`Team "${teamName}" already exists and appears to be owned by a live qwen-code session. Pick a different name, or \u2014 if you're sure no other session is using it \u2014 remove the on-disk artifacts manually:
  rm -rf "${getTeamDir(teamName)}" "${getTasksDir(teamName)}"`;return{llmContent:msg,returnDisplay:msg,error:{message:msg}}}await createTeamFile(teamName,teamFile)}else{throw err}}await resetTaskList(teamName);await clearAllInboxes(teamName);const backend=new InProcessBackend(this.config);await backend.init();const manager=new TeamManager(backend,teamFile,this.config.getSubagentManager(),{maxTeammates:this.config.getAgentsSettings().team?.maxTeammates});this.config.setTeamManager(manager);const ctx={teamName,leadAgentId,teammates:{}};this.config.setTeamContext(ctx);const display={type:"team_result",teamName,action:"created"};const llmContent=`Team "${teamName}" created.`+(this.params.description?` Description: ${this.params.description}`:"");return{llmContent,returnDisplay:display}}};var TeamCreateTool=class _TeamCreateTool extends BaseDeclarativeTool{constructor(config){super(_TeamCreateTool.Name,ToolDisplayNames.TEAM_CREATE,`# TeamCreate

## When to Use

Use this tool proactively whenever:
- The user explicitly asks to use a team, swarm, or group of agents
- The user mentions wanting agents to work together, coordinate, or collaborate
- A task is complex enough that it would benefit from parallel work by multiple agents (e.g., building a full-stack feature with frontend and backend work, refactoring a codebase while keeping tests passing, implementing a multi-step project with research, planning, and coding phases)

When in doubt about whether a task warrants a team, prefer spawning a team.

## Choosing Agent Types for Teammates

When spawning teammates via the Agent tool, choose the \`subagent_type\` based on what tools the agent needs for its task. Each agent type has a different set of available tools \u2014 match the agent to the work:

- **Enforced read-only teammates** use the Agent tool's read_only option. This replaces the selected agent's tool surface with inspection and team-coordination tools, so shell and file writes are unavailable. Use them for research, search, review, or planning tasks.
- **Full-capability agents** (e.g., general-purpose) have access to all tools including file editing, writing, and bash. Use these for tasks that require making changes.
- **Custom agents** defined in \`.qwen/agents/\` may have their own tool restrictions. Check their descriptions to understand what they can and cannot do.

Always review the agent type descriptions and their available tools listed in the Agent tool prompt before selecting a \`subagent_type\` for a teammate.

Create a new team to coordinate multiple agents working on a project. Teams have a 1:1 correspondence with task lists (Team = TaskList).

\`\`\`
{
  "team_name": "my-project",
  "description": "Working on feature X"
}
\`\`\`

This creates:
- A team file at \`~/.qwen/teams/{team-name}/config.json\`
- A corresponding task list directory at \`~/.qwen/tasks/{team-name}/\`

## Team Workflow

1. **Create a team** with TeamCreate - this creates both the team and its task list
2. **Create tasks** using the Task tools (TaskCreate, TaskList, etc.) - they automatically use the team's task list
3. **Spawn teammates** using the Agent tool with explicit \`name\` and \`subagent_type\` parameters to create teammates that join the active team (max ${config.getAgentsSettings().team?.maxTeammates??MAX_TEAMMATES} teammates per team). Set \`read_only: true\` for investigation workers; pin a single writer to a leader-owned worktree with \`working_dir\` when code changes are required, then shut it down before removing that worktree.
4. **Assign tasks** using TaskUpdate with \`owner\` to give tasks to idle teammates
5. **Teammates work on assigned tasks** and mark them completed via TaskUpdate
6. **Teammates go idle between turns** - after each turn, teammates automatically go idle and send a notification. IMPORTANT: Be patient with idle teammates! Don't comment on their idleness until it actually impacts your work.
7. **Shutdown your team** - when the task is completed, gracefully shut down each teammate with ${ToolNames.REQUEST_SHUTDOWN} (\`to\`: the teammate name). ${ToolNames.SEND_MESSAGE} carries ordinary text only.

## Task Ownership

Tasks are assigned using TaskUpdate with the \`owner\` parameter. Any agent can set or change task ownership via TaskUpdate.

## Automatic Message Delivery

**IMPORTANT**: Messages from teammates are automatically delivered to you. You do NOT need to manually check your inbox.

When you spawn teammates:
- They will send you messages when they complete tasks or need help
- These messages appear automatically as new conversation turns (like user messages)
- If you're busy (mid-turn), messages are queued and delivered when your turn ends
- The UI shows a brief notification with the sender's name when messages are waiting

Messages will be delivered automatically.

When reporting on teammate messages, you do NOT need to quote the original message\u2014it's already rendered to the user.

## Teammate Idle State

Teammates go idle after every turn\u2014this is completely normal and expected. A teammate going idle immediately after sending you a message does NOT mean they are done or unavailable. Idle simply means they are waiting for input.

- **Idle teammates can receive messages.** Sending a message to an idle teammate wakes them up and they will process it normally.
- **Idle notifications are automatic.** The system sends an idle notification whenever a teammate's turn ends. You do not need to react to idle notifications unless you want to assign new work or send a follow-up message.
- **Do not treat idle as an error.** A teammate sending a message and then going idle is the normal flow\u2014they sent their message and are now waiting for a response. When a teammate's turn ends, the runtime forwards that teammate's final text output of the turn to you automatically; if they also called send_message earlier, that earlier report is delivered too. There is no summary of teammate-to-teammate messages; ask the teammate directly when you need peer-collaboration detail.

## Discovering Team Members

Teammates can read the team config file to discover other team members:
- **Team config location**: \`~/.qwen/teams/{team-name}/config.json\`

The config file contains a \`members\` array with each teammate's:
- \`name\`: Human-readable name (**always use this** for messaging and task assignment)
- \`agentId\`: Unique identifier (for reference only - do not use for communication)
- \`agentType\`: Role/type of the agent

**IMPORTANT**: Always refer to teammates by their NAME (e.g., "team-lead", "researcher", "tester"). Names are used for:
- \`to\` when sending messages
- Identifying task owners

Example of reading team config:
\`\`\`
Use the Read tool to read ~/.qwen/teams/{team-name}/config.json
\`\`\`

## Task List Coordination

Teams share a task list that all teammates can access at \`~/.qwen/tasks/{team-name}/\`.

Teammates should:
1. Check TaskList periodically, **especially after completing each task**, to find available work or see newly unblocked tasks
2. Claim unassigned, unblocked tasks with TaskUpdate (set \`owner\` to your name). **Prefer tasks in ID order** (lowest ID first) when multiple tasks are available, as earlier tasks often set up context for later ones
3. Create new tasks with \`TaskCreate\` when identifying additional work
4. Mark tasks as completed with \`TaskUpdate\` when done, then check TaskList for next work
5. Coordinate with other teammates by reading the task list status
6. If all available tasks are blocked, notify the team lead or help resolve blocking tasks

**IMPORTANT notes for communication with your team**:
- Do not use terminal tools to view your team's activity; always send a message to your teammates (and remember, refer to them by name).
- Your team cannot hear you if you do not use the SendMessage tool. Always send a message to your teammates if you are responding to them.
- Do NOT send structured JSON status messages like \`{"type":"idle",...}\` or \`{"type":"task_completed",...}\`. Just communicate in plain text when you need to message teammates.
- Use TaskUpdate to mark tasks completed.
- If you are an agent in the team, the system will automatically send idle notifications to the team lead when you stop.`,"other",{type:"object",properties:{team_name:{type:"string",description:"Name for the team (alphanumeric and hyphens)."},description:{type:"string",description:"Optional description of the team."}},required:["team_name"],additionalProperties:false});this.config=config}static{__name(this,"TeamCreateTool")}static Name=ToolNames.TEAM_CREATE;createInvocation(params){return new TeamCreateInvocation(this.config,params)}};export{TeamCreateTool};
/**
 * @license
 * Copyright 2025 Qwen
 * SPDX-License-Identifier: Apache-2.0
 */
