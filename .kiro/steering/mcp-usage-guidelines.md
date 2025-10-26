---
inclusion: always
---

# MCP Tool Usage Guidelines

## File Operations - Use Filesystem MCP Only

**MANDATORY**: Always use Filesystem MCP tools for file operations. Never use built-in file tools.

### Core Filesystem Tools
- `mcp_filesystem_read_text_file` / `mcp_filesystem_read_multiple_files` - Reading files
- `mcp_filesystem_write_file` - Create/overwrite files  
- `mcp_filesystem_edit_file` - Line-based edits with diff output
- `mcp_filesystem_list_directory` / `mcp_filesystem_directory_tree` - Directory operations
- `mcp_filesystem_create_directory` - Create directories
- `mcp_filesystem_move_file` - Move/rename files
- `mcp_filesystem_search_files` - Find files by pattern

**Why**: Provides system-wide access, better error handling, and advanced operations like diff-based editing.

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

## Decision Tree

1. **File operation needed?** → Use Filesystem MCP
2. **Need library docs?** → Use Context7 MCP  
3. **Need web research?** → Use Brave Search MCP
4. **Complex reasoning required?** → Use Sequential Thinking MCP

## Best Practices

- Check MCP tool availability before using built-in alternatives
- Use most specific tool for the task (e.g., news search vs general search)
- Combine tools when beneficial (search + sequential thinking for analysis)
- Leverage auto-approved tools for faster execution