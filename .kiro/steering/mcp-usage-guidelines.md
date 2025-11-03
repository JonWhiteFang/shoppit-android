---
inclusion: always
---

# MCP Tool Usage Guidelines

---

## ⚠️ CRITICAL: File Operations - Use Filesystem MCP EXCLUSIVELY

> **🚨 MANDATORY RULE**: You **MUST ALWAYS** use Filesystem MCP tools for **ALL** file operations. 
> 
> **NEVER** use built-in file tools (readFile, readMultipleFiles, fsWrite, fsAppend, strReplace, deleteFile, etc.).

---

### Why This Rule Exists

| Benefit | Description |
|---------|-------------|
| **System-wide access** | Works across entire filesystem, not just workspace |
| **Better error handling** | More detailed error messages and recovery options |
| **Advanced operations** | Diff-based editing, atomic operations, safer workflows |
| **Consistency** | Single interface for all file operations |
| **Reliability** | More robust and thoroughly tested implementation |

---

### 📊 Tool Comparison: Built-in vs MCP

| Operation | ❌ Built-in Tool (DO NOT USE) | ✅ MCP Tool (USE THIS) |
|-----------|-------------------------------|------------------------|
| Read single file | `readFile` | `mcp_filesystem_read_text_file` |
| Read multiple files | `readMultipleFiles` | `mcp_filesystem_read_multiple_files` |
| Create/overwrite file | `fsWrite` | `mcp_filesystem_write_file` |
| Append to file | `fsAppend` | `mcp_filesystem_edit_file` |
| Replace text | `strReplace` | `mcp_filesystem_edit_file` |
| List directory | `listDirectory` | `mcp_filesystem_list_directory` |
| Search files | `fileSearch` | `mcp_filesystem_search_files` |
| Delete file | `deleteFile` | ⚠️ Not available (intentional safety) |

---

### Filesystem MCP Tools (USE THESE)

#### 📖 Reading Files:
- **`mcp_filesystem_read_text_file`** - Read single file (replaces `readFile`)
- **`mcp_filesystem_read_multiple_files`** - Read multiple files (replaces `readMultipleFiles`)
- **`mcp_filesystem_read_media_file`** - Read images/audio files

#### ✏️ Writing Files:
- **`mcp_filesystem_write_file`** - Create/overwrite files (replaces `fsWrite`)
- **`mcp_filesystem_edit_file`** - Line-based edits with diff output (replaces `strReplace` and `fsAppend`)

#### 📁 Directory Operations:
- **`mcp_filesystem_list_directory`** - List directory contents (replaces `listDirectory`)
- **`mcp_filesystem_list_directory_with_sizes`** - List with file sizes
- **`mcp_filesystem_directory_tree`** - Recursive tree view
- **`mcp_filesystem_create_directory`** - Create directories
- **`mcp_filesystem_search_files`** - Find files by pattern (replaces `fileSearch`)

#### 🔧 File Management:
- **`mcp_filesystem_move_file`** - Move/rename files
- **`mcp_filesystem_get_file_info`** - Get file metadata

---

### ❌ Built-in Tools (DO NOT USE)

| Tool | Status | Replacement |
|------|--------|-------------|
| `readFile` | ❌ **FORBIDDEN** | `mcp_filesystem_read_text_file` |
| `readMultipleFiles` | ❌ **FORBIDDEN** | `mcp_filesystem_read_multiple_files` |
| `fsWrite` | ❌ **FORBIDDEN** | `mcp_filesystem_write_file` |
| `fsAppend` | ❌ **FORBIDDEN** | `mcp_filesystem_edit_file` |
| `strReplace` | ❌ **FORBIDDEN** | `mcp_filesystem_edit_file` |
| `deleteFile` | ❌ **FORBIDDEN** | Not available in MCP (intentional) |
| `listDirectory` | ❌ **FORBIDDEN** | `mcp_filesystem_list_directory` |
| `fileSearch` | ❌ **FORBIDDEN** | `mcp_filesystem_search_files` |

---

### 📝 Examples

#### Reading a file:
```javascript
// ❌ WRONG - DO NOT USE
readFile("path/to/file.md")

// ✅ CORRECT - USE THIS
mcp_filesystem_read_text_file("path/to/file.md")
```

#### Editing a file:
```javascript
// ❌ WRONG - DO NOT USE
strReplace(path, oldStr, newStr)

// ✅ CORRECT - USE THIS
mcp_filesystem_edit_file(path, [{oldText: "...", newText: "..."}])
```

#### Creating a file:
```javascript
// ❌ WRONG - DO NOT USE
fsWrite("path/to/file.md", content)

// ✅ CORRECT - USE THIS
mcp_filesystem_write_file("path/to/file.md", content)
```

#### Listing directory:
```javascript
// ❌ WRONG - DO NOT USE
listDirectory("path/to/dir")

// ✅ CORRECT - USE THIS
mcp_filesystem_list_directory("path/to/dir")
```

---

## 🗺️ MCP Server Selection Guide

### 📚 Documentation Queries → Context7 MCP
**Use when user needs library/framework documentation:**
1. `mcp_Context7_resolve_library_id` - Find library ID
2. `mcp_Context7_get_library_docs` - Fetch current docs

### 🔍 Information Research → Brave Search MCP  
**Use for web-based information:**
- `mcp_brave_search_brave_web_search` - General web search
- `mcp_brave_search_brave_news_search` - Current news/events
- `mcp_brave_search_brave_video_search` - Video content
- `mcp_brave_search_brave_local_search` - Local businesses
- `mcp_brave_search_brave_summarizer` - AI summaries (requires Pro)

### 🧠 Complex Problem Solving → Sequential Thinking MCP
**Use `mcp_sequential_thinking_sequentialthinking` for:**
- Multi-step analysis and planning
- Breaking down complex problems
- Iterative problem refinement

### 🐙 GitHub Operations → GitHub MCP
**Use when working with GitHub repositories:**
- `mcp_github_create_repository` - Create new repositories
- `mcp_github_create_or_update_file` - Create/update single files
- `mcp_github_push_files` - Push multiple files in one commit
- `mcp_github_get_file_contents` - Read repository files
- `mcp_github_create_issue` - Create issues
- `mcp_github_create_pull_request` - Create pull requests
- `mcp_github_create_branch` - Create branches
- `mcp_github_fork_repository` - Fork repositories
- `mcp_github_search_repositories` - Search for repositories
- `mcp_github_search_code` - Search code across GitHub
- `mcp_github_search_issues` - Search issues and PRs
- `mcp_github_list_commits` - List repository commits
- `mcp_github_merge_pull_request` - Merge pull requests

---

## 🌳 Decision Tree

```
┌─────────────────────────────────────┐
│   What do you need to do?          │
└─────────────────────────────────────┘
                 │
    ┌────────────┼────────────┬────────────┬────────────┐
    │            │            │            │            │
    ▼            ▼            ▼            ▼            ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│  File   │ │ Library │ │   Web   │ │ Complex │ │ GitHub  │
│   Op?   │ │  Docs?  │ │Research?│ │Reasoning│ │   Op?   │
└─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
    │            │            │            │            │
    ▼            ▼            ▼            ▼            ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│Filesystem│ │Context7 │ │  Brave  │ │Sequential│ │ GitHub  │
│   MCP   │ │   MCP   │ │  Search │ │Thinking │ │   MCP   │
│(MANDATORY)│ │         │ │   MCP   │ │   MCP   │ │         │
└─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
```

**Priority Order:**
1. **File operation needed?** → **ALWAYS** use Filesystem MCP (**MANDATORY**)
2. **Need library docs?** → Use Context7 MCP  
3. **Need web research?** → Use Brave Search MCP
4. **Complex reasoning required?** → Use Sequential Thinking MCP
5. **GitHub repository operations?** → Use GitHub MCP

---

## 🛡️ Enforcement Rules

### ⚠️ Before ANY File Operation

**Ask yourself these questions:**

| Question | Answer | Action |
|----------|--------|--------|
| Am I using `mcp_filesystem_*` tools? | ✅ Yes | Proceed |
| Am I using built-in file tools? | ❌ Yes | **STOP** - Use MCP instead |

### 🚨 If You Catch Yourself Using Built-in File Tools:

1. **STOP immediately** ⛔
2. **Switch** to the corresponding `mcp_filesystem_*` tool
3. **Continue** with the correct tool

### 🔄 Self-Check Process:

```
Before executing file operation:
  ├─ Is this a file read/write/edit/list operation?
  │   └─ YES → Am I using mcp_filesystem_* ?
  │       ├─ YES → ✅ Proceed
  │       └─ NO → ❌ STOP and switch to MCP
  └─ NO → Proceed with appropriate tool
```

---

## ✅ Best Practices

### Do:
- ✅ **ALWAYS** use Filesystem MCP for file operations (no exceptions)
- ✅ Use most specific tool for the task (e.g., news search vs general search)
- ✅ Combine tools when beneficial (search + sequential thinking for analysis)
- ✅ Leverage auto-approved tools for faster execution
- ✅ Check MCP tool availability before using any alternatives
- ✅ Verify tool selection matches the task requirements

### Don't:
- ❌ **NEVER** use built-in file tools when MCP filesystem is available
- ❌ Don't assume built-in tools are acceptable "just this once"
- ❌ Don't mix built-in and MCP tools in the same workflow
- ❌ Don't skip the self-check process before file operations

---

## 💡 Quick Reference Card

### File Operations (MANDATORY MCP)
```
Read:    mcp_filesystem_read_text_file
Write:   mcp_filesystem_write_file
Edit:    mcp_filesystem_edit_file
List:    mcp_filesystem_list_directory
Search:  mcp_filesystem_search_files
```

### Other MCP Servers
```
Docs:    Context7 MCP
Search:  Brave Search MCP
Think:   Sequential Thinking MCP
GitHub:  GitHub MCP
```

---

## 🎯 Remember

> **The Filesystem MCP rule is absolute and non-negotiable.**
> 
> Every file operation **MUST** use `mcp_filesystem_*` tools.
> 
> No exceptions. No shortcuts. No "just this once."

**When in doubt:**
1. Check if it's a file operation
2. If yes → Use Filesystem MCP
3. If no → Use appropriate MCP server from the decision tree
