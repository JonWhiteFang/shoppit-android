---
inclusion: always
---

# MCP Tool Usage Guidelines

## ⚠️ CRITICAL: File Operations - Use Filesystem MCP EXCLUSIVELY

**MANDATORY RULE**: You MUST ALWAYS use Filesystem MCP tools for ALL file operations. NEVER use built-in file tools (readFile, readMultipleFiles, fsWrite, fsAppend, strReplace, deleteFile, etc.).

### Why This Rule Exists
- **System-wide access** - Works across entire filesystem, not just workspace
- **Better error handling** - More detailed error messages and recovery
- **Advanced operations** - Diff-based editing, atomic operations
- **Consistency** - Single interface for all file operations
- **Reliability** - More robust and tested implementation

### Filesystem MCP Tools (USE THESE)

**Reading Files:**
- `mcp_filesystem_read_text_file` - Read single file (replaces `readFile`)
- `mcp_filesystem_read_multiple_files` - Read multiple files (replaces `readMultipleFiles`)
- `mcp_filesystem_read_media_file` - Read images/audio files

**Writing Files:**
- `mcp_filesystem_write_file` - Create/overwrite files (replaces `fsWrite`)
- `mcp_filesystem_edit_file` - Line-based edits with diff output (replaces `strReplace`)

**Directory Operations:**
- `mcp_filesystem_list_directory` - List directory contents (replaces `listDirectory`)
- `mcp_filesystem_list_directory_with_sizes` - List with file sizes
- `mcp_filesystem_directory_tree` - Recursive tree view
- `mcp_filesystem_create_directory` - Create directories
- `mcp_filesystem_search_files` - Find files by pattern (replaces `fileSearch`)

**File Management:**
- `mcp_filesystem_move_file` - Move/rename files
- `mcp_filesystem_get_file_info` - Get file metadata

### Built-in Tools (DO NOT USE)
❌ `readFile` - Use `mcp_filesystem_read_text_file` instead
❌ `readMultipleFiles` - Use `mcp_filesystem_read_multiple_files` instead
❌ `fsWrite` - Use `mcp_filesystem_write_file` instead
❌ `fsAppend` - Use `mcp_filesystem_edit_file` instead
❌ `strReplace` - Use `mcp_filesystem_edit_file` instead
❌ `deleteFile` - Not available in MCP (intentional safety)
❌ `listDirectory` - Use `mcp_filesystem_list_directory` instead
❌ `fileSearch` - Use `mcp_filesystem_search_files` instead

### Examples

**Reading a file:**
```
// WRONG
readFile("path/to/file.md")

// CORRECT
mcp_filesystem_read_text_file("path/to/file.md")
```

**Editing a file:**
```
// WRONG
strReplace(path, oldStr, newStr)

// CORRECT
mcp_filesystem_edit_file(path, [{oldText: "...", newText: "..."}])
```

**Creating a file:**
```
// WRONG
fsWrite("path/to/file.md", content)

// CORRECT
mcp_filesystem_write_file("path/to/file.md", content)
```

## MCP Server Selection Guide

### Documentation Queries → Context7 MCP
Use when user needs library/framework documentation:
1. `mcp_Context7_resolve_library_id` - Find library ID
2. `mcp_Context7_get_library_docs` - Fetch current docs

### Information Research → Brave Search MCP  
- `mcp_brave_search_brave_web_search` - General web search
- `mcp_brave_search_brave_news_search` - Current news/events
- `mcp_brave_search_brave_video_search` - Video content
- `mcp_brave_search_brave_local_search` - Local businesses
- `mcp_brave_search_brave_summarizer` - AI summaries (requires Pro)

### Complex Problem Solving → Sequential Thinking MCP
Use `mcp_sequential_thinking_sequentialthinking` for:
- Multi-step analysis and planning
- Breaking down complex problems
- Iterative problem refinement

### GitHub Operations → GitHub MCP
Use when working with GitHub repositories:
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

## Decision Tree

1. **File operation needed?** → ALWAYS use Filesystem MCP (MANDATORY)
2. **Need library docs?** → Use Context7 MCP  
3. **Need web research?** → Use Brave Search MCP
4. **Complex reasoning required?** → Use Sequential Thinking MCP
5. **GitHub repository operations?** → Use GitHub MCP

## Enforcement

**Before ANY file operation, ask yourself:**
- Am I using `mcp_filesystem_*` tools? ✅
- Am I using built-in file tools? ❌ STOP - Use MCP instead

**If you catch yourself using built-in file tools:**
1. Stop immediately
2. Switch to the corresponding `mcp_filesystem_*` tool
3. Continue with the correct tool

## Best Practices

- ✅ Always use Filesystem MCP for file operations (no exceptions)
- ✅ Use most specific tool for the task (e.g., news search vs general search)
- ✅ Combine tools when beneficial (search + sequential thinking for analysis)
- ✅ Leverage auto-approved tools for faster execution
- ✅ Check MCP tool availability before using any alternatives
- ❌ Never use built-in file tools when MCP filesystem is available
