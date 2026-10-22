// packages/core/src/sandbox/file-worker-runner.ts
import { mkdir } from "node:fs/promises";
import path6 from "node:path";

// packages/core/src/utils/atomicFileWrite.ts
import * as crypto2 from "node:crypto";
import * as fsSync from "node:fs";
import * as fs4 from "node:fs/promises";
import * as path4 from "node:path";

// packages/core/src/utils/debugLogger.ts
import { promises as fs3 } from "node:fs";
import path3 from "node:path";
import { AsyncLocalStorage as AsyncLocalStorage3 } from "node:async_hooks";
import util from "node:util";

// packages/core/src/config/storage.ts
import * as path from "node:path";
import * as os2 from "node:os";
import * as fs from "node:fs";
import { AsyncLocalStorage } from "node:async_hooks";

// packages/core/src/utils/paths.ts
import os from "node:os";
import * as crypto from "node:crypto";

// packages/core/src/utils/errors.ts
function isNodeError(error) {
  return error instanceof Error && "code" in error;
}
var FatalError = class extends Error {
  constructor(message, exitCode) {
    super(message);
    this.exitCode = exitCode;
  }
};
var FatalConfigError = class extends FatalError {
  constructor(message) {
    super(message, 52);
  }
};

// packages/core/src/utils/paths.ts
var QWEN_DIR = ".qwen";
var SHELL_SPECIAL_CHARS = /[ \t()[\]{};|*?$`'"#&<>!~,]/;
var UNESCAPE_REGEX = (() => {
  const inner = SHELL_SPECIAL_CHARS.source.slice(1, -1);
  return new RegExp(`\\\\([${inner}])`, "g");
})();
function getProjectHash(projectRoot) {
  const normalizedPath = os.platform() === "win32" ? projectRoot.toLowerCase() : projectRoot;
  return crypto.createHash("sha256").update(normalizedPath).digest("hex");
}
function sanitizeCwd(cwd) {
  const normalizedCwd = os.platform() === "win32" ? cwd.toLowerCase() : cwd;
  return normalizedCwd.replace(/[^a-zA-Z0-9]/g, "-");
}

// packages/core/src/config/storage.ts
var GOOGLE_ACCOUNTS_FILENAME = "google_accounts.json";
var OAUTH_FILE = "oauth_creds.json";
var SKILL_PROVIDER_CONFIG_DIRS = [".qwen", ".agents"];
var TMP_DIR_NAME = "tmp";
var BIN_DIR_NAME = "bin";
var PROJECT_DIR_NAME = "projects";
var IDE_DIR_NAME = "ide";
var PLANS_DIR_NAME = "plans";
var DEBUG_DIR_NAME = "debug";
var ARENA_DIR_NAME = "arena";
function platformFoldsCase() {
  return process.platform === "win32" || process.platform === "darwin";
}
function isResolvedPathWithinDirectory(childPath, parentPath) {
  const child = platformFoldsCase() ? childPath.toLowerCase() : childPath;
  const parent = platformFoldsCase() ? parentPath.toLowerCase() : parentPath;
  const relativePath = path.relative(parent, child);
  return relativePath === "" || !relativePath.startsWith(`..${path.sep}`) && relativePath !== ".." && !path.isAbsolute(relativePath);
}
var Storage = class _Storage {
  targetDir;
  runtimeBaseDir;
  /**
   * Custom runtime output base directory set via settings.
   * When null, falls back to getGlobalQwenDir().
   */
  static runtimeBaseDir = null;
  static runtimeBaseDirContext = new AsyncLocalStorage();
  constructor(targetDir, runtimeBaseDir = _Storage.getRuntimeBaseDir()) {
    this.targetDir = targetDir;
    this.runtimeBaseDir = path.resolve(runtimeBaseDir);
  }
  /**
   * Expands tilde and resolves relative paths to absolute.
   */
  static resolvePath(dir, cwd) {
    let resolved = dir;
    if (resolved === "~" || resolved.startsWith("~/") || resolved.startsWith("~\\")) {
      const relativeSegments = resolved === "~" ? [] : resolved.slice(2).split(/[/\\]+/).filter(Boolean);
      resolved = path.join(os2.homedir(), ...relativeSegments);
    }
    if (!path.isAbsolute(resolved)) {
      resolved = cwd ? path.resolve(cwd, resolved) : path.resolve(resolved);
    }
    return resolved;
  }
  /**
   * Sanitizes a session id for use as a plan filename.
   *
   * Plan files are keyed by session id, but the raw id is public SDK input.
   * Strip directory separators and Windows-invalid filename characters so a
   * hostile value cannot escape the plans directory.
   */
  static sanitizePlanSessionId(sessionId) {
    const safeName = path.basename(sessionId.replace(/\\/g, "/")).replace(/^\.+/g, "_").replace(/[<>:"|?*\x00-\x1F]/g, "_");
    return safeName || "_";
  }
  static resolveRuntimeBaseDir(dir, cwd) {
    if (!dir) {
      return null;
    }
    return _Storage.resolvePath(dir, cwd);
  }
  /**
   * Sets the custom runtime output base directory.
   * Handles tilde (~) expansion and resolves relative paths to absolute.
   * Pass null/undefined/empty string to reset to default (getGlobalQwenDir()).
   * @param dir - The directory path, or null/undefined to reset
   * @param cwd - Base directory for resolving relative paths (defaults to process.cwd()).
   *              Pass the project root so that relative values like ".qwen" resolve
   *              per-project, enabling a single global config to work across all projects.
   */
  static setRuntimeBaseDir(dir, cwd) {
    _Storage.runtimeBaseDir = _Storage.resolveRuntimeBaseDir(dir, cwd);
  }
  /**
   * Runs function execution in an async context with a specific runtime output dir.
   * This is used to isolate runtime output paths between concurrent sessions.
   */
  static runWithRuntimeBaseDir(dir, cwd, fn) {
    if (_Storage.runtimeBaseDirContext.getStore()?.pinned) {
      return fn();
    }
    const resolved = _Storage.resolveRuntimeBaseDir(dir, cwd);
    return _Storage.runtimeBaseDirContext.run(
      { dir: resolved, pinned: false },
      fn
    );
  }
  static runWithResolvedRuntimeBaseDir(dir, fn) {
    return _Storage.runtimeBaseDirContext.run(
      { dir: path.resolve(dir), pinned: true },
      fn
    );
  }
  static hasRuntimeBaseDirContext() {
    return _Storage.runtimeBaseDirContext.getStore() !== void 0;
  }
  /**
   * Returns the base directory for all runtime output (temp files, debug logs,
   * session data, todos, insights, etc.).
   *
   * Priority: pinned runtime context > QWEN_RUNTIME_DIR env var > configurable context > setRuntimeBaseDir() value > getGlobalQwenDir()
   * @returns Absolute path to the runtime output base directory
   */
  static getRuntimeBaseDir() {
    const contextualDir = _Storage.runtimeBaseDirContext.getStore();
    if (contextualDir?.pinned) {
      return contextualDir.dir ?? _Storage.getGlobalQwenDir();
    }
    const envDir = process.env["QWEN_RUNTIME_DIR"];
    if (envDir) {
      return _Storage.resolveRuntimeBaseDir(envDir) ?? _Storage.getGlobalQwenDir();
    }
    if (contextualDir !== void 0) {
      return contextualDir.dir ?? _Storage.getGlobalQwenDir();
    }
    if (_Storage.runtimeBaseDir) {
      return _Storage.runtimeBaseDir;
    }
    return _Storage.getGlobalQwenDir();
  }
  static getGlobalQwenDir() {
    const envDir = process.env["QWEN_HOME"];
    if (envDir) {
      return _Storage.resolvePath(envDir);
    }
    const homeDir = os2.homedir();
    if (!homeDir) {
      return path.join(os2.tmpdir(), ".qwen");
    }
    return path.join(homeDir, QWEN_DIR);
  }
  static getMcpOAuthTokensPath() {
    return path.join(_Storage.getGlobalQwenDir(), "mcp-oauth-tokens.json");
  }
  static getGlobalSettingsPath() {
    return path.join(_Storage.getGlobalQwenDir(), "settings.json");
  }
  static getInstallationIdPath() {
    return path.join(_Storage.getGlobalQwenDir(), "installation_id");
  }
  static getGoogleAccountsPath() {
    return path.join(_Storage.getGlobalQwenDir(), GOOGLE_ACCOUNTS_FILENAME);
  }
  static getUserCommandsDir() {
    return path.join(_Storage.getGlobalQwenDir(), "commands");
  }
  static getGlobalMemoryFilePath() {
    return path.join(_Storage.getGlobalQwenDir(), "memory.md");
  }
  static getGlobalTempDir() {
    return path.join(_Storage.getRuntimeBaseDir(), TMP_DIR_NAME);
  }
  static getGlobalDebugDir() {
    return path.join(_Storage.getRuntimeBaseDir(), DEBUG_DIR_NAME);
  }
  static getDebugLogPath(sessionId) {
    return path.join(_Storage.getGlobalDebugDir(), `${sessionId}.txt`);
  }
  static getGlobalIdeDir() {
    return path.join(_Storage.getGlobalQwenDir(), IDE_DIR_NAME);
  }
  /**
   * Resolves pathToResolve by realpathing its deepest existing ancestor and
   * appending the not-yet-created remainder.
   */
  static resolvePathThroughExistingAncestor(pathToResolve) {
    let candidate = pathToResolve;
    while (true) {
      try {
        const realCandidate = fs.realpathSync(candidate);
        const remainder = path.relative(candidate, pathToResolve);
        return path.join(realCandidate, remainder);
      } catch (err) {
        if (err.code !== "ENOENT") {
          throw err;
        }
        const parent = path.dirname(candidate);
        if (parent === candidate) {
          return pathToResolve;
        }
        candidate = parent;
      }
    }
  }
  /**
   * Checks whether {@link childPath} resides within {@link parentPath},
   * resolving symbolic links to prevent traversal bypass attacks.
   */
  static isPathWithinDirectory(childPath, parentPath) {
    const realParent = _Storage.resolvePathThroughExistingAncestor(parentPath);
    const realChild = _Storage.resolvePathThroughExistingAncestor(childPath);
    return isResolvedPathWithinDirectory(realChild, realParent);
  }
  static assertPathWithinDirectory(childPath, parentPath, errorMessage) {
    if (!_Storage.isPathWithinDirectory(childPath, parentPath)) {
      throw new FatalConfigError(errorMessage);
    }
  }
  static getPlansDir(projectRoot, plansDirectory) {
    const configuredPlansDirectory = plansDirectory?.trim();
    if (configuredPlansDirectory) {
      if (!projectRoot) {
        throw new FatalConfigError(
          "projectRoot is required when plansDirectory is configured."
        );
      }
      const resolvedProjectRoot = path.resolve(projectRoot);
      const resolvedPlansDirectory = _Storage.resolvePath(
        configuredPlansDirectory,
        resolvedProjectRoot
      );
      _Storage.assertPathWithinDirectory(
        resolvedPlansDirectory,
        resolvedProjectRoot,
        `plansDirectory must resolve within the project root.`
      );
      return resolvedPlansDirectory;
    }
    return path.join(_Storage.getGlobalQwenDir(), PLANS_DIR_NAME);
  }
  static getPlanFilePath(sessionId, projectRoot, plansDirectory) {
    return path.join(
      _Storage.getPlansDir(projectRoot, plansDirectory),
      `${_Storage.sanitizePlanSessionId(sessionId)}.md`
    );
  }
  static getGlobalBinDir() {
    return path.join(_Storage.getGlobalQwenDir(), BIN_DIR_NAME);
  }
  static getGlobalArenaDir() {
    return path.join(_Storage.getGlobalQwenDir(), ARENA_DIR_NAME);
  }
  /**
   * Create or adopt the outside-repo landing for /audit reports and sidecars
   * when the audited repository's ignore state cannot keep them out of
   * version control. Per-user and per-project, honoring the QWEN_HOME
   * override; 0700 on POSIX so the quoted (possibly exploitable) module
   * content stays private to the user.
   */
  static ensureAuditFallbackDir(projectRoot) {
    let resolved = projectRoot;
    try {
      resolved = fs.realpathSync(projectRoot);
    } catch {
    }
    if (platformFoldsCase()) {
      resolved = resolved.toLowerCase();
    }
    const baseDir = _Storage.getGlobalQwenDir();
    const dir = path.join(baseDir, "audits", getProjectHash(resolved));
    _Storage.assertAuditLandingIsOutsideRepo(dir, resolved);
    try {
      fs.mkdirSync(baseDir, { recursive: true });
    } catch (err) {
      throw new FatalConfigError(
        `audit: the QWEN_HOME base ${baseDir} could not be created as a directory (${err.message}) \u2014 remove what stands at that path and re-run.`
      );
    }
    _Storage.assertAuditLandingIsOutsideRepo(dir, resolved);
    const auditsDir = _Storage.adoptDirectory(
      path.join(baseDir, "audits"),
      "the audit artifact directory"
    );
    _Storage.adoptDirectory(dir, "the fallback landing");
    _Storage.assertAuditLandingIsClean(dir);
    _Storage.adoptDirectory(auditsDir, "the audit artifact directory");
    _Storage.adoptDirectory(dir, "the fallback landing");
    _Storage.assertAuditLandingIsClean(dir);
    _Storage.assertAuditLandingIsOutsideRepo(dir, resolved, true);
    _Storage.adoptDirectory(auditsDir, "the audit artifact directory");
    _Storage.adoptDirectory(dir, "the fallback landing");
    return dir;
  }
  /**
   * Create one path component and return it only if what is there now is a
   * real directory (not a symlink). On POSIX a pre-existing component is
   * tightened to 0700; ownership itself is not checked.
   *
   * Non-recursive on purpose: `recursive: true` would silently walk (and
   * follow) anything already standing in the path. Creating exactly one
   * component at a time is what makes each component checkable.
   */
  static adoptDirectory(dir, what) {
    try {
      fs.mkdirSync(dir, { mode: 448 });
    } catch (err) {
      if (err.code !== "EEXIST") throw err;
    }
    const stat2 = fs.lstatSync(dir);
    if (!stat2.isDirectory()) {
      throw new FatalConfigError(
        `audit: ${what} ${dir} is not a directory (it may be a symlink planted ahead of the run) \u2014 remove it and re-run.`
      );
    }
    if (process.platform !== "win32" && (stat2.mode & 511) !== 448) {
      fs.chmodSync(dir, 448);
    }
    return dir;
  }
  /**
   * Refuse a fallback landing whose CONTENTS would redirect writes out of it.
   *
   * Validating the leaf alone is not enough: artifacts land at paths BELOW it
   * (`audit-<ts>.sidecar/sidecar.json`, the dated report), and an
   * O_NOFOLLOW open only ever guards the final component. A planted symlink
   * child is therefore a complete escape — `mkdirSync` happily treats a
   * symlink-to-directory as the directory, and every artifact written
   * "inside" the landing lands wherever the link points, while the leaf
   * itself stays a perfectly valid directory that re-validation passes.
   * A hardlinked regular file is the same story for reads: an existing name
   * reopened with O_TRUNC writes into the planter's inode.
   *
   * Directory children are validated recursively: a planted real
   * subdirectory holding a symlinked file is the same escape. Entries that
   * are neither regular files nor directories (a FIFO, socket, or device)
   * are refused outright: opening one for the report would block, or stream
   * the content to whoever holds the other end.
   *
   * The landing is REUSED across runs (the report and its sidecar are the
   * durable artifacts), so this cannot refuse a non-empty landing — only
   * entries that are not what a previous run of this tool would have left.
   */
  static assertAuditLandingIsClean(dir) {
    let entries;
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch (err) {
      throw new FatalConfigError(
        `audit: the fallback landing ${dir} could not be listed for validation (${err.message}) \u2014 remove it and re-run.`
      );
    }
    for (const entry of entries) {
      let stat2;
      try {
        stat2 = fs.lstatSync(path.join(dir, entry.name));
      } catch {
        continue;
      }
      if (stat2.isSymbolicLink()) {
        throw new FatalConfigError(
          `audit: the fallback landing ${dir} contains a symlink (${entry.name}) \u2014 artifacts written under it would land outside the landing. Remove it and re-run.`
        );
      }
      if (stat2.isDirectory()) {
        _Storage.assertAuditLandingIsClean(path.join(dir, entry.name));
        let childStat;
        try {
          childStat = fs.lstatSync(path.join(dir, entry.name));
        } catch {
          continue;
        }
        if (!childStat.isDirectory()) {
          throw new FatalConfigError(
            `audit: the fallback landing ${dir} contains a symlink (${entry.name}) \u2014 artifacts written under it would land outside the landing. Remove it and re-run.`
          );
        }
        continue;
      }
      if (!stat2.isFile()) {
        throw new FatalConfigError(
          `audit: the fallback landing ${dir} contains a special file (${entry.name}) \u2014 a write to it would block or be captured by whoever holds the other end. Remove it and re-run.`
        );
      }
      if (stat2.nlink > 1) {
        throw new FatalConfigError(
          `audit: the fallback landing ${dir} contains a hardlinked file (${entry.name}) \u2014 a write to it would also write through its twin. Remove it and re-run.`
        );
      }
    }
  }
  /**
   * Refuse a fallback landing that resolves inside the audited repository.
   * A resolution failure (a non-directory component, a symlink loop, an
   * unreadable ancestor) falls through at the pre-adoption sites: such a
   * path cannot resolve to a usable landing, and the adoption checks that
   * still run afterwards own that state with the actionable message. At the
   * final site nothing runs afterwards, so the same failure fails closed
   * instead of returning an unvalidated landing.
   */
  static assertAuditLandingIsOutsideRepo(dir, resolvedProjectRoot, finalCheck = false) {
    let contained = false;
    try {
      contained = _Storage.isPathWithinDirectory(dir, resolvedProjectRoot);
    } catch (err) {
      if (finalCheck) {
        throw new FatalConfigError(
          `audit: the fallback landing ${dir} could not be validated as outside the audited project root (${err.message}) \u2014 remove it and re-run.`
        );
      }
    }
    if (contained) {
      throw new FatalConfigError(
        `audit: the fallback landing ${dir} resolves inside the audited project root \u2014 point QWEN_HOME outside the repository and re-run.`
      );
    }
  }
  getQwenDir() {
    return path.join(this.targetDir, QWEN_DIR);
  }
  getRuntimeBaseDir() {
    return this.runtimeBaseDir;
  }
  getProjectDir() {
    const projectId = sanitizeCwd(this.getProjectRoot());
    const projectsDir = path.join(this.runtimeBaseDir, PROJECT_DIR_NAME);
    return path.join(projectsDir, projectId);
  }
  getProjectTempDir() {
    const hash = getProjectHash(this.getProjectRoot());
    const tempDir = path.join(this.runtimeBaseDir, TMP_DIR_NAME);
    const targetDir = path.join(tempDir, hash);
    return targetDir;
  }
  getToolResultsDir() {
    return path.join(this.getProjectTempDir(), "tool-results");
  }
  ensureProjectTempDirExists() {
    fs.mkdirSync(this.getProjectTempDir(), { recursive: true });
  }
  static getOAuthCredsPath() {
    return path.join(_Storage.getGlobalQwenDir(), OAUTH_FILE);
  }
  getProjectRoot() {
    return this.targetDir;
  }
  getWorkspaceSettingsPath() {
    return path.join(this.getQwenDir(), "settings.json");
  }
  getProjectCommandsDir() {
    return path.join(this.getQwenDir(), "commands");
  }
  /**
   * Project-level saved-workflow scripts directory: `<targetDir>/.qwen/workflows`.
   * Saved workflow scripts (`<name>.js`) here are surfaced as slash commands
   * and resolvable by `workflow('<name>')` from inside a running workflow.
   */
  getProjectWorkflowsDir() {
    return path.join(this.getQwenDir(), "workflows");
  }
  /**
   * User-level saved-workflow scripts directory: `~/.qwen/workflows`. User
   * scope is lower-precedence than project scope when the same `<name>.js`
   * exists in both.
   */
  static getUserWorkflowsDir() {
    return path.join(_Storage.getGlobalQwenDir(), "workflows");
  }
  /**
   * Per-run workflow artifact directory: `<projectDir>/workflows`. Holds
   * completed-run snapshot JSON files (`<runId>.json`) for the `/workflows`
   * recent list, and per-run resume journals (`<runId>/journal.jsonl`).
   */
  getWorkflowRunsDir() {
    return path.join(this.getProjectDir(), "workflows");
  }
  /**
   * Generated-workflow scripts directory: `<projectDir>/workflows/generated`.
   * A trusted root for `Workflow({scriptPath})` / `workflow({scriptPath})`
   * that is NOT a saved-workflow scope: scripts here are never listed as
   * `/<name>` slash commands and cannot be reached by `workflow('<name>')`.
   * It exists for tooling that emits a script for one run (a CLI subcommand
   * generating a fan-out for the model to dispatch) — such a script has no
   * business in the user's command namespace, and the runtime dir keeps it
   * out of the project tree. Layout below the root is the writer's; the
   * loader trusts the whole subtree. A subprocess reaches it as
   * `$QWEN_CODE_PROJECT_DIR/workflows/generated`.
   */
  getGeneratedWorkflowsDir() {
    return path.join(this.getWorkflowRunsDir(), "generated");
  }
  /**
   * Where an inline `Workflow({script})` run persists its script:
   * `<generated>/inline/<runId>.js`. Inside the generated root because that
   * root is already a trusted `{scriptPath}` source and is never resolvable
   * by name, so the persisted copy is re-runnable (and editable before a
   * resume) without entering the user's command namespace. The `inline/`
   * subdirectory keeps model-authored scripts apart from the one-run scripts
   * other tooling emits into the same root.
   */
  getInlineWorkflowScriptPath(runId) {
    return path.join(this.getGeneratedWorkflowsDir(), "inline", `${runId}.js`);
  }
  /**
   * Path to the persisted snapshot of a completed workflow run.
   */
  getWorkflowRunSnapshotPath(runId) {
    return path.join(this.getWorkflowRunsDir(), `${runId}.json`);
  }
  /**
   * Path to the resume journal for an in-progress / resumable workflow run.
   */
  getWorkflowRunJournalPath(runId) {
    return path.join(this.getWorkflowRunsDir(), runId, "journal.jsonl");
  }
  /**
   * Path to the runtime-status sidecar JSON for this session.
   *
   * Co-located with the per-session chat log under
   * `<projectDir>/chats/<sessionId>.runtime.json` so external observers
   * (terminal multiplexers, IDE integrations, status daemons) can scan
   * the same directory used for chat history to find live sessions.
   */
  getRuntimeStatusPath(sessionId) {
    return path.join(
      this.getProjectDir(),
      "chats",
      `${sessionId}.runtime.json`
    );
  }
  getProjectTempCheckpointsDir() {
    return path.join(this.getProjectTempDir(), "checkpoints");
  }
  getExtensionsDir() {
    return path.join(this.getQwenDir(), "extensions");
  }
  getExtensionsConfigPath() {
    return path.join(this.getExtensionsDir(), "qwen-extension.json");
  }
  getUserSkillsDirs() {
    const homeDir = os2.homedir() || os2.tmpdir();
    return SKILL_PROVIDER_CONFIG_DIRS.map(
      (dir) => dir === QWEN_DIR ? path.join(_Storage.getGlobalQwenDir(), "skills") : path.join(homeDir, dir, "skills")
    );
  }
  /**
   * Returns the user-level extensions directory (~/.qwen/extensions/).
   * Extensions installed at user scope are stored here, as opposed to
   * project-level extensions which live in <project>/.qwen/extensions/.
   */
  static getUserExtensionsDir() {
    return path.join(_Storage.getGlobalQwenDir(), "extensions");
  }
  getHistoryFilePath() {
    return path.join(this.getProjectTempDir(), "shell_history");
  }
};

// packages/core/src/utils/symlink.ts
import { promises as fs2 } from "node:fs";
import path2 from "node:path";
async function updateSymlink(linkPath, targetPath, options) {
  const { fallbackCopy = true } = options ?? {};
  const linkDir = path2.dirname(linkPath);
  const relativeTarget = path2.relative(linkDir, targetPath);
  try {
    await fs2.unlink(linkPath);
  } catch {
  }
  try {
    await fs2.symlink(relativeTarget, linkPath);
    return;
  } catch {
  }
  if (fallbackCopy) {
    try {
      await fs2.copyFile(targetPath, linkPath);
    } catch {
    }
  }
}

// node_modules/@opentelemetry/api/build/esm/platform/node/globalThis.js
var _globalThis = typeof globalThis === "object" ? globalThis : global;

// node_modules/@opentelemetry/api/build/esm/version.js
var VERSION = "1.9.0";

// node_modules/@opentelemetry/api/build/esm/internal/semver.js
var re = /^(\d+)\.(\d+)\.(\d+)(-(.+))?$/;
function _makeCompatibilityCheck(ownVersion) {
  var acceptedVersions = /* @__PURE__ */ new Set([ownVersion]);
  var rejectedVersions = /* @__PURE__ */ new Set();
  var myVersionMatch = ownVersion.match(re);
  if (!myVersionMatch) {
    return function() {
      return false;
    };
  }
  var ownVersionParsed = {
    major: +myVersionMatch[1],
    minor: +myVersionMatch[2],
    patch: +myVersionMatch[3],
    prerelease: myVersionMatch[4]
  };
  if (ownVersionParsed.prerelease != null) {
    return function isExactmatch(globalVersion) {
      return globalVersion === ownVersion;
    };
  }
  function _reject(v) {
    rejectedVersions.add(v);
    return false;
  }
  function _accept(v) {
    acceptedVersions.add(v);
    return true;
  }
  return function isCompatible2(globalVersion) {
    if (acceptedVersions.has(globalVersion)) {
      return true;
    }
    if (rejectedVersions.has(globalVersion)) {
      return false;
    }
    var globalVersionMatch = globalVersion.match(re);
    if (!globalVersionMatch) {
      return _reject(globalVersion);
    }
    var globalVersionParsed = {
      major: +globalVersionMatch[1],
      minor: +globalVersionMatch[2],
      patch: +globalVersionMatch[3],
      prerelease: globalVersionMatch[4]
    };
    if (globalVersionParsed.prerelease != null) {
      return _reject(globalVersion);
    }
    if (ownVersionParsed.major !== globalVersionParsed.major) {
      return _reject(globalVersion);
    }
    if (ownVersionParsed.major === 0) {
      if (ownVersionParsed.minor === globalVersionParsed.minor && ownVersionParsed.patch <= globalVersionParsed.patch) {
        return _accept(globalVersion);
      }
      return _reject(globalVersion);
    }
    if (ownVersionParsed.minor <= globalVersionParsed.minor) {
      return _accept(globalVersion);
    }
    return _reject(globalVersion);
  };
}
var isCompatible = _makeCompatibilityCheck(VERSION);

// node_modules/@opentelemetry/api/build/esm/internal/global-utils.js
var major = VERSION.split(".")[0];
var GLOBAL_OPENTELEMETRY_API_KEY = Symbol.for("opentelemetry.js.api." + major);
var _global = _globalThis;
function registerGlobal(type, instance, diag, allowOverride) {
  var _a;
  if (allowOverride === void 0) {
    allowOverride = false;
  }
  var api = _global[GLOBAL_OPENTELEMETRY_API_KEY] = (_a = _global[GLOBAL_OPENTELEMETRY_API_KEY]) !== null && _a !== void 0 ? _a : {
    version: VERSION
  };
  if (!allowOverride && api[type]) {
    var err = new Error("@opentelemetry/api: Attempted duplicate registration of API: " + type);
    diag.error(err.stack || err.message);
    return false;
  }
  if (api.version !== VERSION) {
    var err = new Error("@opentelemetry/api: Registration of version v" + api.version + " for " + type + " does not match previously registered API v" + VERSION);
    diag.error(err.stack || err.message);
    return false;
  }
  api[type] = instance;
  diag.debug("@opentelemetry/api: Registered a global for " + type + " v" + VERSION + ".");
  return true;
}
function getGlobal(type) {
  var _a, _b;
  var globalVersion = (_a = _global[GLOBAL_OPENTELEMETRY_API_KEY]) === null || _a === void 0 ? void 0 : _a.version;
  if (!globalVersion || !isCompatible(globalVersion)) {
    return;
  }
  return (_b = _global[GLOBAL_OPENTELEMETRY_API_KEY]) === null || _b === void 0 ? void 0 : _b[type];
}
function unregisterGlobal(type, diag) {
  diag.debug("@opentelemetry/api: Unregistering a global for " + type + " v" + VERSION + ".");
  var api = _global[GLOBAL_OPENTELEMETRY_API_KEY];
  if (api) {
    delete api[type];
  }
}

// node_modules/@opentelemetry/api/build/esm/diag/ComponentLogger.js
var __read = function(o, n) {
  var m = typeof Symbol === "function" && o[Symbol.iterator];
  if (!m) return o;
  var i = m.call(o), r, ar = [], e;
  try {
    while ((n === void 0 || n-- > 0) && !(r = i.next()).done) ar.push(r.value);
  } catch (error) {
    e = { error };
  } finally {
    try {
      if (r && !r.done && (m = i["return"])) m.call(i);
    } finally {
      if (e) throw e.error;
    }
  }
  return ar;
};
var __spreadArray = function(to, from, pack) {
  if (pack || arguments.length === 2) for (var i = 0, l = from.length, ar; i < l; i++) {
    if (ar || !(i in from)) {
      if (!ar) ar = Array.prototype.slice.call(from, 0, i);
      ar[i] = from[i];
    }
  }
  return to.concat(ar || Array.prototype.slice.call(from));
};
var DiagComponentLogger = (
  /** @class */
  (function() {
    function DiagComponentLogger2(props) {
      this._namespace = props.namespace || "DiagComponentLogger";
    }
    DiagComponentLogger2.prototype.debug = function() {
      var args = [];
      for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
      }
      return logProxy("debug", this._namespace, args);
    };
    DiagComponentLogger2.prototype.error = function() {
      var args = [];
      for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
      }
      return logProxy("error", this._namespace, args);
    };
    DiagComponentLogger2.prototype.info = function() {
      var args = [];
      for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
      }
      return logProxy("info", this._namespace, args);
    };
    DiagComponentLogger2.prototype.warn = function() {
      var args = [];
      for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
      }
      return logProxy("warn", this._namespace, args);
    };
    DiagComponentLogger2.prototype.verbose = function() {
      var args = [];
      for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
      }
      return logProxy("verbose", this._namespace, args);
    };
    return DiagComponentLogger2;
  })()
);
function logProxy(funcName, namespace, args) {
  var logger = getGlobal("diag");
  if (!logger) {
    return;
  }
  args.unshift(namespace);
  return logger[funcName].apply(logger, __spreadArray([], __read(args), false));
}

// node_modules/@opentelemetry/api/build/esm/diag/types.js
var DiagLogLevel;
(function(DiagLogLevel2) {
  DiagLogLevel2[DiagLogLevel2["NONE"] = 0] = "NONE";
  DiagLogLevel2[DiagLogLevel2["ERROR"] = 30] = "ERROR";
  DiagLogLevel2[DiagLogLevel2["WARN"] = 50] = "WARN";
  DiagLogLevel2[DiagLogLevel2["INFO"] = 60] = "INFO";
  DiagLogLevel2[DiagLogLevel2["DEBUG"] = 70] = "DEBUG";
  DiagLogLevel2[DiagLogLevel2["VERBOSE"] = 80] = "VERBOSE";
  DiagLogLevel2[DiagLogLevel2["ALL"] = 9999] = "ALL";
})(DiagLogLevel || (DiagLogLevel = {}));

// node_modules/@opentelemetry/api/build/esm/diag/internal/logLevelLogger.js
function createLogLevelDiagLogger(maxLevel, logger) {
  if (maxLevel < DiagLogLevel.NONE) {
    maxLevel = DiagLogLevel.NONE;
  } else if (maxLevel > DiagLogLevel.ALL) {
    maxLevel = DiagLogLevel.ALL;
  }
  logger = logger || {};
  function _filterFunc(funcName, theLevel) {
    var theFunc = logger[funcName];
    if (typeof theFunc === "function" && maxLevel >= theLevel) {
      return theFunc.bind(logger);
    }
    return function() {
    };
  }
  return {
    error: _filterFunc("error", DiagLogLevel.ERROR),
    warn: _filterFunc("warn", DiagLogLevel.WARN),
    info: _filterFunc("info", DiagLogLevel.INFO),
    debug: _filterFunc("debug", DiagLogLevel.DEBUG),
    verbose: _filterFunc("verbose", DiagLogLevel.VERBOSE)
  };
}

// node_modules/@opentelemetry/api/build/esm/api/diag.js
var __read2 = function(o, n) {
  var m = typeof Symbol === "function" && o[Symbol.iterator];
  if (!m) return o;
  var i = m.call(o), r, ar = [], e;
  try {
    while ((n === void 0 || n-- > 0) && !(r = i.next()).done) ar.push(r.value);
  } catch (error) {
    e = { error };
  } finally {
    try {
      if (r && !r.done && (m = i["return"])) m.call(i);
    } finally {
      if (e) throw e.error;
    }
  }
  return ar;
};
var __spreadArray2 = function(to, from, pack) {
  if (pack || arguments.length === 2) for (var i = 0, l = from.length, ar; i < l; i++) {
    if (ar || !(i in from)) {
      if (!ar) ar = Array.prototype.slice.call(from, 0, i);
      ar[i] = from[i];
    }
  }
  return to.concat(ar || Array.prototype.slice.call(from));
};
var API_NAME = "diag";
var DiagAPI = (
  /** @class */
  (function() {
    function DiagAPI2() {
      function _logProxy(funcName) {
        return function() {
          var args = [];
          for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
          }
          var logger = getGlobal("diag");
          if (!logger)
            return;
          return logger[funcName].apply(logger, __spreadArray2([], __read2(args), false));
        };
      }
      var self = this;
      var setLogger = function(logger, optionsOrLogLevel) {
        var _a, _b, _c;
        if (optionsOrLogLevel === void 0) {
          optionsOrLogLevel = { logLevel: DiagLogLevel.INFO };
        }
        if (logger === self) {
          var err = new Error("Cannot use diag as the logger for itself. Please use a DiagLogger implementation like ConsoleDiagLogger or a custom implementation");
          self.error((_a = err.stack) !== null && _a !== void 0 ? _a : err.message);
          return false;
        }
        if (typeof optionsOrLogLevel === "number") {
          optionsOrLogLevel = {
            logLevel: optionsOrLogLevel
          };
        }
        var oldLogger = getGlobal("diag");
        var newLogger = createLogLevelDiagLogger((_b = optionsOrLogLevel.logLevel) !== null && _b !== void 0 ? _b : DiagLogLevel.INFO, logger);
        if (oldLogger && !optionsOrLogLevel.suppressOverrideMessage) {
          var stack = (_c = new Error().stack) !== null && _c !== void 0 ? _c : "<failed to generate stacktrace>";
          oldLogger.warn("Current logger will be overwritten from " + stack);
          newLogger.warn("Current logger will overwrite one already registered from " + stack);
        }
        return registerGlobal("diag", newLogger, self, true);
      };
      self.setLogger = setLogger;
      self.disable = function() {
        unregisterGlobal(API_NAME, self);
      };
      self.createComponentLogger = function(options) {
        return new DiagComponentLogger(options);
      };
      self.verbose = _logProxy("verbose");
      self.debug = _logProxy("debug");
      self.info = _logProxy("info");
      self.warn = _logProxy("warn");
      self.error = _logProxy("error");
    }
    DiagAPI2.instance = function() {
      if (!this._instance) {
        this._instance = new DiagAPI2();
      }
      return this._instance;
    };
    return DiagAPI2;
  })()
);

// node_modules/@opentelemetry/api/build/esm/context/context.js
function createContextKey(description) {
  return Symbol.for(description);
}
var BaseContext = (
  /** @class */
  /* @__PURE__ */ (function() {
    function BaseContext2(parentContext) {
      var self = this;
      self._currentContext = parentContext ? new Map(parentContext) : /* @__PURE__ */ new Map();
      self.getValue = function(key) {
        return self._currentContext.get(key);
      };
      self.setValue = function(key, value) {
        var context = new BaseContext2(self._currentContext);
        context._currentContext.set(key, value);
        return context;
      };
      self.deleteValue = function(key) {
        var context = new BaseContext2(self._currentContext);
        context._currentContext.delete(key);
        return context;
      };
    }
    return BaseContext2;
  })()
);
var ROOT_CONTEXT = new BaseContext();

// node_modules/@opentelemetry/api/build/esm/context/NoopContextManager.js
var __read3 = function(o, n) {
  var m = typeof Symbol === "function" && o[Symbol.iterator];
  if (!m) return o;
  var i = m.call(o), r, ar = [], e;
  try {
    while ((n === void 0 || n-- > 0) && !(r = i.next()).done) ar.push(r.value);
  } catch (error) {
    e = { error };
  } finally {
    try {
      if (r && !r.done && (m = i["return"])) m.call(i);
    } finally {
      if (e) throw e.error;
    }
  }
  return ar;
};
var __spreadArray3 = function(to, from, pack) {
  if (pack || arguments.length === 2) for (var i = 0, l = from.length, ar; i < l; i++) {
    if (ar || !(i in from)) {
      if (!ar) ar = Array.prototype.slice.call(from, 0, i);
      ar[i] = from[i];
    }
  }
  return to.concat(ar || Array.prototype.slice.call(from));
};
var NoopContextManager = (
  /** @class */
  (function() {
    function NoopContextManager2() {
    }
    NoopContextManager2.prototype.active = function() {
      return ROOT_CONTEXT;
    };
    NoopContextManager2.prototype.with = function(_context, fn, thisArg) {
      var args = [];
      for (var _i = 3; _i < arguments.length; _i++) {
        args[_i - 3] = arguments[_i];
      }
      return fn.call.apply(fn, __spreadArray3([thisArg], __read3(args), false));
    };
    NoopContextManager2.prototype.bind = function(_context, target) {
      return target;
    };
    NoopContextManager2.prototype.enable = function() {
      return this;
    };
    NoopContextManager2.prototype.disable = function() {
      return this;
    };
    return NoopContextManager2;
  })()
);

// node_modules/@opentelemetry/api/build/esm/api/context.js
var __read4 = function(o, n) {
  var m = typeof Symbol === "function" && o[Symbol.iterator];
  if (!m) return o;
  var i = m.call(o), r, ar = [], e;
  try {
    while ((n === void 0 || n-- > 0) && !(r = i.next()).done) ar.push(r.value);
  } catch (error) {
    e = { error };
  } finally {
    try {
      if (r && !r.done && (m = i["return"])) m.call(i);
    } finally {
      if (e) throw e.error;
    }
  }
  return ar;
};
var __spreadArray4 = function(to, from, pack) {
  if (pack || arguments.length === 2) for (var i = 0, l = from.length, ar; i < l; i++) {
    if (ar || !(i in from)) {
      if (!ar) ar = Array.prototype.slice.call(from, 0, i);
      ar[i] = from[i];
    }
  }
  return to.concat(ar || Array.prototype.slice.call(from));
};
var API_NAME2 = "context";
var NOOP_CONTEXT_MANAGER = new NoopContextManager();
var ContextAPI = (
  /** @class */
  (function() {
    function ContextAPI2() {
    }
    ContextAPI2.getInstance = function() {
      if (!this._instance) {
        this._instance = new ContextAPI2();
      }
      return this._instance;
    };
    ContextAPI2.prototype.setGlobalContextManager = function(contextManager) {
      return registerGlobal(API_NAME2, contextManager, DiagAPI.instance());
    };
    ContextAPI2.prototype.active = function() {
      return this._getContextManager().active();
    };
    ContextAPI2.prototype.with = function(context, fn, thisArg) {
      var _a;
      var args = [];
      for (var _i = 3; _i < arguments.length; _i++) {
        args[_i - 3] = arguments[_i];
      }
      return (_a = this._getContextManager()).with.apply(_a, __spreadArray4([context, fn, thisArg], __read4(args), false));
    };
    ContextAPI2.prototype.bind = function(context, target) {
      return this._getContextManager().bind(context, target);
    };
    ContextAPI2.prototype._getContextManager = function() {
      return getGlobal(API_NAME2) || NOOP_CONTEXT_MANAGER;
    };
    ContextAPI2.prototype.disable = function() {
      this._getContextManager().disable();
      unregisterGlobal(API_NAME2, DiagAPI.instance());
    };
    return ContextAPI2;
  })()
);

// node_modules/@opentelemetry/api/build/esm/trace/trace_flags.js
var TraceFlags;
(function(TraceFlags2) {
  TraceFlags2[TraceFlags2["NONE"] = 0] = "NONE";
  TraceFlags2[TraceFlags2["SAMPLED"] = 1] = "SAMPLED";
})(TraceFlags || (TraceFlags = {}));

// node_modules/@opentelemetry/api/build/esm/trace/invalid-span-constants.js
var INVALID_SPANID = "0000000000000000";
var INVALID_TRACEID = "00000000000000000000000000000000";
var INVALID_SPAN_CONTEXT = {
  traceId: INVALID_TRACEID,
  spanId: INVALID_SPANID,
  traceFlags: TraceFlags.NONE
};

// node_modules/@opentelemetry/api/build/esm/trace/NonRecordingSpan.js
var NonRecordingSpan = (
  /** @class */
  (function() {
    function NonRecordingSpan2(_spanContext) {
      if (_spanContext === void 0) {
        _spanContext = INVALID_SPAN_CONTEXT;
      }
      this._spanContext = _spanContext;
    }
    NonRecordingSpan2.prototype.spanContext = function() {
      return this._spanContext;
    };
    NonRecordingSpan2.prototype.setAttribute = function(_key, _value) {
      return this;
    };
    NonRecordingSpan2.prototype.setAttributes = function(_attributes) {
      return this;
    };
    NonRecordingSpan2.prototype.addEvent = function(_name, _attributes) {
      return this;
    };
    NonRecordingSpan2.prototype.addLink = function(_link) {
      return this;
    };
    NonRecordingSpan2.prototype.addLinks = function(_links) {
      return this;
    };
    NonRecordingSpan2.prototype.setStatus = function(_status) {
      return this;
    };
    NonRecordingSpan2.prototype.updateName = function(_name) {
      return this;
    };
    NonRecordingSpan2.prototype.end = function(_endTime) {
    };
    NonRecordingSpan2.prototype.isRecording = function() {
      return false;
    };
    NonRecordingSpan2.prototype.recordException = function(_exception, _time) {
    };
    return NonRecordingSpan2;
  })()
);

// node_modules/@opentelemetry/api/build/esm/trace/context-utils.js
var SPAN_KEY = createContextKey("OpenTelemetry Context Key SPAN");
function getSpan(context) {
  return context.getValue(SPAN_KEY) || void 0;
}
function getActiveSpan() {
  return getSpan(ContextAPI.getInstance().active());
}
function setSpan(context, span) {
  return context.setValue(SPAN_KEY, span);
}
function deleteSpan(context) {
  return context.deleteValue(SPAN_KEY);
}
function setSpanContext(context, spanContext) {
  return setSpan(context, new NonRecordingSpan(spanContext));
}
function getSpanContext(context) {
  var _a;
  return (_a = getSpan(context)) === null || _a === void 0 ? void 0 : _a.spanContext();
}

// node_modules/@opentelemetry/api/build/esm/trace/spancontext-utils.js
var VALID_TRACEID_REGEX = /^([0-9a-f]{32})$/i;
var VALID_SPANID_REGEX = /^[0-9a-f]{16}$/i;
function isValidTraceId(traceId) {
  return VALID_TRACEID_REGEX.test(traceId) && traceId !== INVALID_TRACEID;
}
function isValidSpanId(spanId) {
  return VALID_SPANID_REGEX.test(spanId) && spanId !== INVALID_SPANID;
}
function isSpanContextValid(spanContext) {
  return isValidTraceId(spanContext.traceId) && isValidSpanId(spanContext.spanId);
}
function wrapSpanContext(spanContext) {
  return new NonRecordingSpan(spanContext);
}

// node_modules/@opentelemetry/api/build/esm/trace/NoopTracer.js
var contextApi = ContextAPI.getInstance();
var NoopTracer = (
  /** @class */
  (function() {
    function NoopTracer2() {
    }
    NoopTracer2.prototype.startSpan = function(name, options, context) {
      if (context === void 0) {
        context = contextApi.active();
      }
      var root = Boolean(options === null || options === void 0 ? void 0 : options.root);
      if (root) {
        return new NonRecordingSpan();
      }
      var parentFromContext = context && getSpanContext(context);
      if (isSpanContext(parentFromContext) && isSpanContextValid(parentFromContext)) {
        return new NonRecordingSpan(parentFromContext);
      } else {
        return new NonRecordingSpan();
      }
    };
    NoopTracer2.prototype.startActiveSpan = function(name, arg2, arg3, arg4) {
      var opts;
      var ctx;
      var fn;
      if (arguments.length < 2) {
        return;
      } else if (arguments.length === 2) {
        fn = arg2;
      } else if (arguments.length === 3) {
        opts = arg2;
        fn = arg3;
      } else {
        opts = arg2;
        ctx = arg3;
        fn = arg4;
      }
      var parentContext = ctx !== null && ctx !== void 0 ? ctx : contextApi.active();
      var span = this.startSpan(name, opts, parentContext);
      var contextWithSpanSet = setSpan(parentContext, span);
      return contextApi.with(contextWithSpanSet, fn, void 0, span);
    };
    return NoopTracer2;
  })()
);
function isSpanContext(spanContext) {
  return typeof spanContext === "object" && typeof spanContext["spanId"] === "string" && typeof spanContext["traceId"] === "string" && typeof spanContext["traceFlags"] === "number";
}

// node_modules/@opentelemetry/api/build/esm/trace/ProxyTracer.js
var NOOP_TRACER = new NoopTracer();
var ProxyTracer = (
  /** @class */
  (function() {
    function ProxyTracer2(_provider, name, version, options) {
      this._provider = _provider;
      this.name = name;
      this.version = version;
      this.options = options;
    }
    ProxyTracer2.prototype.startSpan = function(name, options, context) {
      return this._getTracer().startSpan(name, options, context);
    };
    ProxyTracer2.prototype.startActiveSpan = function(_name, _options, _context, _fn) {
      var tracer = this._getTracer();
      return Reflect.apply(tracer.startActiveSpan, tracer, arguments);
    };
    ProxyTracer2.prototype._getTracer = function() {
      if (this._delegate) {
        return this._delegate;
      }
      var tracer = this._provider.getDelegateTracer(this.name, this.version, this.options);
      if (!tracer) {
        return NOOP_TRACER;
      }
      this._delegate = tracer;
      return this._delegate;
    };
    return ProxyTracer2;
  })()
);

// node_modules/@opentelemetry/api/build/esm/trace/NoopTracerProvider.js
var NoopTracerProvider = (
  /** @class */
  (function() {
    function NoopTracerProvider2() {
    }
    NoopTracerProvider2.prototype.getTracer = function(_name, _version, _options) {
      return new NoopTracer();
    };
    return NoopTracerProvider2;
  })()
);

// node_modules/@opentelemetry/api/build/esm/trace/ProxyTracerProvider.js
var NOOP_TRACER_PROVIDER = new NoopTracerProvider();
var ProxyTracerProvider = (
  /** @class */
  (function() {
    function ProxyTracerProvider2() {
    }
    ProxyTracerProvider2.prototype.getTracer = function(name, version, options) {
      var _a;
      return (_a = this.getDelegateTracer(name, version, options)) !== null && _a !== void 0 ? _a : new ProxyTracer(this, name, version, options);
    };
    ProxyTracerProvider2.prototype.getDelegate = function() {
      var _a;
      return (_a = this._delegate) !== null && _a !== void 0 ? _a : NOOP_TRACER_PROVIDER;
    };
    ProxyTracerProvider2.prototype.setDelegate = function(delegate) {
      this._delegate = delegate;
    };
    ProxyTracerProvider2.prototype.getDelegateTracer = function(name, version, options) {
      var _a;
      return (_a = this._delegate) === null || _a === void 0 ? void 0 : _a.getTracer(name, version, options);
    };
    return ProxyTracerProvider2;
  })()
);

// node_modules/@opentelemetry/api/build/esm/api/trace.js
var API_NAME3 = "trace";
var TraceAPI = (
  /** @class */
  (function() {
    function TraceAPI2() {
      this._proxyTracerProvider = new ProxyTracerProvider();
      this.wrapSpanContext = wrapSpanContext;
      this.isSpanContextValid = isSpanContextValid;
      this.deleteSpan = deleteSpan;
      this.getSpan = getSpan;
      this.getActiveSpan = getActiveSpan;
      this.getSpanContext = getSpanContext;
      this.setSpan = setSpan;
      this.setSpanContext = setSpanContext;
    }
    TraceAPI2.getInstance = function() {
      if (!this._instance) {
        this._instance = new TraceAPI2();
      }
      return this._instance;
    };
    TraceAPI2.prototype.setGlobalTracerProvider = function(provider) {
      var success = registerGlobal(API_NAME3, this._proxyTracerProvider, DiagAPI.instance());
      if (success) {
        this._proxyTracerProvider.setDelegate(provider);
      }
      return success;
    };
    TraceAPI2.prototype.getTracerProvider = function() {
      return getGlobal(API_NAME3) || this._proxyTracerProvider;
    };
    TraceAPI2.prototype.getTracer = function(name, version) {
      return this.getTracerProvider().getTracer(name, version);
    };
    TraceAPI2.prototype.disable = function() {
      unregisterGlobal(API_NAME3, DiagAPI.instance());
      this._proxyTracerProvider = new ProxyTracerProvider();
    };
    return TraceAPI2;
  })()
);

// node_modules/@opentelemetry/api/build/esm/trace-api.js
var trace = TraceAPI.getInstance();

// packages/core/src/telemetry/session-context.ts
var sessionIdContextKey = createContextKey("qwen-code.telemetry.session-id");
var sessionRootContext;
function getSessionContext() {
  return sessionRootContext;
}

// packages/core/src/telemetry/trace-context.ts
function extractTraceContext(span) {
  const ctx = span?.spanContext();
  if (ctx && isSpanContextValid(ctx)) {
    return {
      traceId: ctx.traceId,
      spanId: ctx.spanId,
      traceFlags: ctx.traceFlags
    };
  }
  return null;
}
function getActiveSpanTraceContext() {
  try {
    return extractTraceContext(trace.getActiveSpan());
  } catch {
    return null;
  }
}
function getSessionRootTraceContext() {
  try {
    const sessionCtx = getSessionContext();
    return extractTraceContext(
      sessionCtx ? trace.getSpan(sessionCtx) : void 0
    );
  } catch {
    return null;
  }
}
function getTraceContext() {
  return getActiveSpanTraceContext() ?? getSessionRootTraceContext();
}

// packages/core/src/utils/sessionIdContext.ts
import { AsyncLocalStorage as AsyncLocalStorage2 } from "node:async_hooks";
var sessionIdContext = new AsyncLocalStorage2();

// packages/core/src/utils/debugLogger.ts
var ensureDebugDirPromise = null;
var ensuredDebugDirPath = null;
var hasWriteFailure = false;
var globalSession = null;
var lastAliasedKey = null;
var aliasGeneration = 0;
var aliasFailureStreak = 0;
var aliasChain = Promise.resolve();
var sessionContext = new AsyncLocalStorage3();
function isDebugLogFileEnabled() {
  const value = process.env["QWEN_DEBUG_LOG_FILE"];
  if (!value) return false;
  const normalized = value.trim().toLowerCase();
  return !["", "0", "false", "off", "no"].includes(normalized);
}
function getActiveSession() {
  const contextSession = sessionContext.getStore();
  if (contextSession === false) return null;
  if (contextSession) return contextSession;
  const sessionId = sessionIdContext.getStore();
  if (sessionId) {
    return { getSessionId: () => sessionId };
  }
  return globalSession;
}
function ensureDebugDirExists() {
  const debugDirPath = Storage.getGlobalDebugDir();
  if (!ensureDebugDirPromise || ensuredDebugDirPath !== debugDirPath) {
    ensuredDebugDirPath = debugDirPath;
    ensureDebugDirPromise = fs3.mkdir(debugDirPath, { recursive: true }).then(() => void 0).catch(() => {
      hasWriteFailure = true;
      ensureDebugDirPromise = null;
      ensuredDebugDirPath = null;
    });
  }
  return ensureDebugDirPromise ?? Promise.resolve();
}
function formatArgs(args) {
  return args.map((arg) => {
    if (arg instanceof Error) {
      return arg.stack ?? `${arg.name}: ${arg.message}`;
    }
    return arg;
  }).map((arg) => typeof arg === "string" ? arg : util.inspect(arg)).join(" ");
}
function buildLogLine(level, message, tag, traceCtx) {
  const timestamp = (/* @__PURE__ */ new Date()).toISOString();
  const tagPart = tag ? ` [${tag}]` : "";
  const tracePart = traceCtx ? ` [trace_id=${traceCtx.traceId} span_id=${traceCtx.spanId}]` : "";
  return `${timestamp} [${level}]${tagPart}${tracePart} ${message}
`;
}
function writeLog(session, level, tag, args) {
  if (!isDebugLogFileEnabled()) {
    return;
  }
  const sessionId = session.getSessionId();
  const logFilePath = Storage.getDebugLogPath(sessionId);
  const message = formatArgs(args);
  const traceCtx = getTraceContext();
  const line = buildLogLine(level, message, tag, traceCtx);
  updateLatestDebugLogAlias(sessionId);
  void ensureDebugDirExists().then(() => fs3.appendFile(logFilePath, line, "utf8")).catch(() => {
    hasWriteFailure = true;
  });
}
var DEBUG_LATEST_ALIAS = "latest";
var SESSION_ID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
var MAX_CONSECUTIVE_ALIAS_FAILURES = 3;
async function doUpdateLatestDebugLogAlias(sessionId) {
  const aliasPath = path3.join(Storage.getGlobalDebugDir(), DEBUG_LATEST_ALIAS);
  const targetPath = Storage.getDebugLogPath(sessionId);
  await ensureDebugDirExists();
  await updateSymlink(aliasPath, targetPath, { fallbackCopy: false });
  try {
    const actualTarget = await fs3.readlink(aliasPath);
    return actualTarget === path3.relative(path3.dirname(aliasPath), targetPath);
  } catch {
    return false;
  }
}
function updateLatestDebugLogAlias(sessionId) {
  if (!isDebugLogFileEnabled()) {
    return;
  }
  if (!SESSION_ID_PATTERN.test(sessionId)) {
    return;
  }
  const key = path3.join(Storage.getGlobalDebugDir(), sessionId);
  if (key === lastAliasedKey) {
    return;
  }
  lastAliasedKey = key;
  const generation = ++aliasGeneration;
  aliasChain = aliasChain.then(async () => {
    let updated = false;
    try {
      updated = await doUpdateLatestDebugLogAlias(sessionId);
    } catch {
    }
    if (updated) {
      aliasFailureStreak = 0;
      return;
    }
    aliasFailureStreak += 1;
    if (aliasFailureStreak < MAX_CONSECUTIVE_ALIAS_FAILURES && aliasGeneration === generation) {
      lastAliasedKey = null;
    }
  });
}
function createDebugLogger(tag) {
  return {
    isEnabled: () => getActiveSession() !== null,
    debug: (...args) => {
      const session = getActiveSession();
      if (!session) return;
      writeLog(session, "DEBUG", tag, args);
    },
    info: (...args) => {
      const session = getActiveSession();
      if (!session) return;
      writeLog(session, "INFO", tag, args);
    },
    warn: (...args) => {
      const session = getActiveSession();
      if (!session) return;
      writeLog(session, "WARN", tag, args);
    },
    error: (...args) => {
      const session = getActiveSession();
      if (!session) return;
      writeLog(session, "ERROR", tag, args);
    }
  };
}

// packages/core/src/utils/atomicFileWrite.ts
var debugLogger = createDebugLogger("ATOMIC_WRITE");
async function renameWithRetry(src, dest, retries, delayMs, _renameImpl = fs4.rename) {
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      await _renameImpl(src, dest);
      return;
    } catch (error) {
      const isRetryable = isNodeError(error) && (error.code === "EPERM" || error.code === "EACCES");
      if (!isRetryable || attempt === retries) {
        throw error;
      }
      await new Promise(
        (resolve3) => setTimeout(resolve3, delayMs * 2 ** attempt)
      );
    }
  }
}
async function resolveSymlinkChain(filePath) {
  const maxHops = 40;
  let current = filePath;
  for (let i = 0; i < maxHops; i++) {
    let lstats;
    try {
      lstats = await fs4.lstat(current);
    } catch (err2) {
      if (isNodeError(err2) && err2.code === "ENOENT") {
        return current;
      }
      throw err2;
    }
    if (!lstats.isSymbolicLink()) {
      return current;
    }
    const linkTarget = await fs4.readlink(current);
    if (path4.isAbsolute(linkTarget)) {
      current = linkTarget;
    } else {
      const parentDir = await fs4.realpath(path4.dirname(current));
      current = path4.resolve(parentDir, linkTarget);
    }
  }
  const err = new Error(
    `ELOOP: too many levels of symbolic links, resolve '${filePath}'`
  );
  err.code = "ELOOP";
  throw err;
}
async function atomicWriteFile(filePath, data, options, _testFs) {
  const retries = options?.retries ?? 3;
  const delayMs = options?.delayMs ?? 50;
  const flush = options?.flush ?? true;
  const encoding = options?.encoding ?? "utf-8";
  const renameImpl = _testFs?.rename ?? fs4.rename;
  const writeFileImpl = _testFs?.writeFile ?? fs4.writeFile;
  const openImpl = _testFs?.open ?? fs4.open;
  const chmodImpl = _testFs?.chmod ?? fs4.chmod;
  const fchmodImpl = _testFs?.fchmod ?? ((fh, mode) => fh.chmod(mode));
  const unlinkImpl = _testFs?.unlink ?? fs4.unlink;
  const targetPath = options?.noFollow ? filePath : await resolveSymlinkChain(filePath).catch((err) => {
    throw annotateWriteError(err, filePath);
  });
  let existingStat;
  try {
    existingStat = options?.noFollow ? await fs4.lstat(targetPath) : await fs4.stat(targetPath);
  } catch (err) {
    if (!isNodeError(err) || err.code !== "ENOENT") {
      throw err;
    }
  }
  let existingMode;
  if (!options?.forceMode || options?.mode === void 0) {
    existingMode = existingStat !== void 0 && (!options?.noFollow || existingStat.isFile()) ? existingStat.mode & 4095 : void 0;
  }
  const desiredMode = existingMode ?? options?.mode;
  const writeOptions = {};
  if (typeof data === "string") writeOptions.encoding = encoding;
  if (flush) writeOptions.flush = true;
  if (desiredMode !== void 0) writeOptions.mode = desiredMode;
  const tryChmod = async (target) => {
    if (desiredMode === void 0) return;
    try {
      await chmodImpl(target, desiredMode);
    } catch (chmodErr) {
      if (!isNodeError(chmodErr) || chmodErr.code !== "ENOSYS" && chmodErr.code !== "ENOTSUP") {
        throw chmodErr;
      }
    }
  };
  const ownershipWouldChange = () => {
    if (existingStat === void 0) return false;
    if (process.platform === "win32") return false;
    const euid = process.geteuid?.();
    if (euid === void 0) return false;
    return existingStat.uid !== euid;
  };
  if (existingStat !== void 0 && existingStat.isFile() && ownershipWouldChange()) {
    options?.assertCanCommit?.();
    await fs4.writeFile(targetPath, data, writeOptions);
    await tryChmod(targetPath);
    return;
  }
  const tmpPath = `${targetPath}.${crypto2.randomBytes(6).toString("hex")}.tmp`;
  try {
    await writeFileImpl(tmpPath, data, writeOptions);
    await tryChmod(tmpPath);
    await renameWithRetry(
      tmpPath,
      targetPath,
      retries,
      delayMs,
      async (src, dest) => {
        options?.assertCanCommit?.();
        await renameImpl(src, dest);
      }
    );
  } catch (error) {
    try {
      await unlinkImpl(tmpPath);
    } catch {
    }
    if (isNodeError(error) && error.code === "EXDEV") {
      try {
        options?.assertCanCommit?.();
        if (options?.noFollow) {
          try {
            await unlinkImpl(targetPath);
          } catch (unlinkErr) {
            if (!isNodeError(unlinkErr) || unlinkErr.code !== "ENOENT") {
              throw unlinkErr;
            }
          }
          const fd = await openImpl(
            targetPath,
            fsSync.constants.O_WRONLY | fsSync.constants.O_CREAT | fsSync.constants.O_EXCL,
            desiredMode ?? 438
          );
          let writeOk = false;
          try {
            try {
              await fd.writeFile(
                typeof data === "string" ? Buffer.from(data, encoding) : data
              );
              if (flush) await fd.sync();
              if (desiredMode !== void 0) {
                try {
                  await fchmodImpl(fd, desiredMode);
                } catch (chmodErr) {
                  if (!isNodeError(chmodErr) || chmodErr.code !== "ENOSYS" && chmodErr.code !== "ENOTSUP") {
                    throw chmodErr;
                  }
                }
              }
              writeOk = true;
            } finally {
              await fd.close();
            }
          } catch (writeErr) {
            if (!writeOk) {
              try {
                await unlinkImpl(targetPath);
              } catch (orphanErr) {
                debugLogger.debug(
                  `orphan unlink failed for ${targetPath}:`,
                  orphanErr
                );
              }
            }
            throw writeErr;
          }
        } else {
          await writeFileImpl(targetPath, data, writeOptions);
          await tryChmod(targetPath);
        }
        return;
      } catch (fallbackError) {
        throw annotateWriteError(fallbackError, targetPath);
      }
    }
    throw annotateWriteError(error, targetPath);
  }
}
function annotateWriteError(error, targetPath, fnName = "atomicWriteFile") {
  if (error instanceof Error && !error.message.startsWith(`${fnName}(`)) {
    error.message = `${fnName}(${JSON.stringify(targetPath)}): ${error.message}`;
  }
  return error;
}

// packages/core/src/sandbox/file-version.ts
import { statSync as statSync2 } from "node:fs";
var VERSION_FIELDS = ["dev", "ino", "size", "mtimeNs", "ctimeNs"];
function isSandboxFileVersion(value) {
  return value !== null && typeof value === "object" && VERSION_FIELDS.every((key) => {
    const field = value[key];
    return typeof field === "string" && /^-?\d{1,40}$/.test(field);
  });
}
function getSandboxFileVersion(filePath) {
  try {
    const stats = statSync2(filePath, { bigint: true });
    if (!stats.isFile()) {
      throw Object.assign(new Error("File tools require a regular file."), {
        code: stats.isDirectory() ? "EISDIR" : "EINVAL"
      });
    }
    return Object.freeze({
      dev: stats.dev.toString(),
      ino: stats.ino.toString(),
      size: stats.size.toString(),
      mtimeNs: stats.mtimeNs.toString(),
      ctimeNs: stats.ctimeNs.toString()
    });
  } catch (error) {
    if (error.code === "ENOENT") return null;
    throw error;
  }
}
function assertSandboxFileVersion(filePath, expected) {
  const current = getSandboxFileVersion(filePath);
  if (current === null ? expected !== null : expected === null || VERSION_FIELDS.some((key) => current[key] !== expected[key])) {
    throw Object.assign(
      new Error(
        "File changed since it was prepared. Re-read it before retrying."
      ),
      { code: "ESTALE" }
    );
  }
}

// packages/core/src/sandbox/file-worker-protocol.ts
import path5 from "node:path";
var MAX_FILE_HEADER_BYTES = 16 * 1024;
async function readSandboxWriteRequest(input) {
  const chunks = [];
  let headerBytes = 0;
  let headerEnd = -1;
  for await (const value of input) {
    const chunk = Buffer.from(value);
    if (headerEnd === -1) {
      const newline = chunk.indexOf(10);
      if (newline !== -1) headerEnd = headerBytes + newline;
      headerBytes += newline === -1 ? chunk.length : newline + 1;
      if (headerBytes > MAX_FILE_HEADER_BYTES)
        throw new Error("File worker header exceeds 16 KiB.");
    }
    chunks.push(chunk);
  }
  if (headerEnd === -1) throw new Error("Missing file worker header.");
  const wire = Buffer.concat(chunks);
  const header = JSON.parse(
    wire.subarray(0, headerEnd).toString("utf8")
  );
  if (!header || typeof header !== "object")
    throw new Error("Invalid write request.");
  const record = header;
  if (record["operation"] !== "write" || typeof record["destination"] !== "string" || !path5.isAbsolute(record["destination"]) || !(record["expected"] === null || isSandboxFileVersion(record["expected"])) || typeof record["contentLength"] !== "number" || !Number.isSafeInteger(record["contentLength"]) || record["contentLength"] < 0)
    throw new Error("Invalid write request.");
  const content = wire.subarray(headerEnd + 1);
  if (content.length !== record["contentLength"])
    throw new Error("Truncated file worker body.");
  return {
    operation: "write",
    destination: record["destination"],
    expected: record["expected"],
    content
  };
}

// packages/core/src/sandbox/file-worker-runner.ts
async function runFileWorker(input, writeReply) {
  try {
    const request = await readSandboxWriteRequest(input);
    const assertCanCommit = () => assertSandboxFileVersion(request.destination, request.expected);
    assertCanCommit();
    await mkdir(path6.dirname(request.destination), { recursive: true });
    await atomicWriteFile(request.destination, request.content, {
      assertCanCommit
    });
    writeReply(JSON.stringify({ ok: true }) + "\n");
    return 0;
  } catch (error) {
    writeReply(
      JSON.stringify({
        ok: false,
        error: error instanceof Error ? error.message : String(error),
        code: error?.code
      }) + "\n"
    );
    return 1;
  }
}

// packages/core/src/sandbox/file-worker.ts
process.exitCode = await runFileWorker(process.stdin, (reply) => {
  process.stdout.write(reply);
});
/**
 * @license
 * Copyright 2025 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * @license
 * Copyright 2025 Qwen Code
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * @license
 * Copyright 2025 Qwen
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * @license
 * Copyright 2025 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * @license
 * Copyright 2026 Qwen Team
 * SPDX-License-Identifier: Apache-2.0
 */
