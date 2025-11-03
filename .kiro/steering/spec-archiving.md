---
inclusion: manual
---

# Spec Archiving Workflow

## Automatic Archiving Rule

**CRITICAL**: When starting work on a new spec from `shoppit-specs-tracker.md`, you MUST first archive all completed specs.

## Archiving Process

### Step 1: Identify Completed Specs
Check `shoppit-specs-tracker.md` for specs marked with `[x]` (completed).

### Step 2: Archive Each Completed Spec
For each completed spec:

1. **Move the spec directory** to `.kiro/specs/archived/`
   ```bash
   # Example for meal-management spec
   mv .kiro/specs/meal-management .kiro/specs/archived/meal-management
   ```

2. **Preserve the directory structure** - keep all files intact

3. **Update the tracker** - Add archive reference to completed items

### Step 3: Update Tracker Format

Modify completed spec entries to include archive reference:

```markdown
## Phase 1: Core Foundation
- [x] **project-setup** - Project structure and dependencies → [archived](.kiro/specs/archived/project-setup)
- [x] **meal-management** - Core meal CRUD operations → [archived](.kiro/specs/archived/meal-management)
```

### Step 4: Verify Archive
Confirm that:
- Completed spec directories are in `.kiro/specs/archived/`
- Active/incomplete specs remain in `.kiro/specs/`
- Tracker has archive links for completed specs

## When to Archive

### Automatic Archiving Triggers

Archive completed specs in these situations:

1. **Before starting a new spec** from the tracker
   - User says: "Start the [spec-name] spec"
   - User says: "Begin work on [spec-name]"
   - User says: "Let's work on [spec-name]"

2. **When explicitly requested** by the user
   - User says: "Archive completed specs"
   - User says: "Clean up the specs directory"
   - User says: "Move finished specs to archive"

3. **During project cleanup** or reorganization
   - User says: "Organize the project"
   - User says: "Clean up the workspace"

4. **When spec directory is cluttered**
   - More than 3 completed specs in main directory
   - User is having trouble finding active specs

### What Qualifies as "Completed"

A spec is ready for archiving when:
- ✅ Marked with `[x]` in `shoppit-specs-tracker.md`
- ✅ All tasks in `tasks.md` are completed
- ✅ Implementation is merged to main/develop branch
- ✅ No active work or pending changes

### What Should NOT Be Archived

Do not archive specs that are:
- ❌ Marked with `[ ]` (incomplete) in tracker
- ❌ Have pending tasks in `tasks.md`
- ❌ Currently being worked on
- ❌ Have unmerged changes
- ❌ Marked as "In Progress" or "Blocked"

## Archive Structure

### Directory Layout

```
.kiro/specs/
├── archived/                           # All completed specs
│   ├── project-setup/                  # Phase 1 - Completed
│   │   ├── requirements.md
│   │   ├── design.md
│   │   └── tasks.md
│   ├── meal-management/                # Phase 1 - Completed
│   │   ├── requirements.md
│   │   ├── design.md
│   │   └── tasks.md
│   ├── data-persistence/               # Phase 1 - Completed
│   │   ├── requirements.md
│   │   ├── design.md
│   │   └── tasks.md
│   ├── meal-planning/                  # Phase 2 - Completed
│   │   ├── requirements.md
│   │   ├── design.md
│   │   └── tasks.md
│   └── shopping-list-generation/       # Phase 2 - Completed
│       ├── requirements.md
│       ├── design.md
│       └── tasks.md
├── error-handling-and-validation/      # Active - In Progress
│   ├── requirements.md
│   ├── design.md
│   └── tasks.md
├── performance-optimization/           # Pending - Not Started
│   └── (empty or initial files)
├── steering-docs-refinement/           # Active - In Progress
│   ├── requirements.md
│   ├── design.md
│   └── tasks.md
└── shoppit-specs-tracker.md            # Master tracker
```

### Tracker Format with Archive Links

```markdown
# Shoppit Specs Tracker

## Phase 1: Core Foundation
- [x] **project-setup** - Project structure and dependencies → [archived](.kiro/specs/archived/project-setup)
- [x] **meal-management** - Core meal CRUD operations → [archived](.kiro/specs/archived/meal-management)
- [x] **data-persistence** - Room database setup → [archived](.kiro/specs/archived/data-persistence)

## Phase 2: Meal Planning
- [x] **meal-planning** - Weekly meal planner → [archived](.kiro/specs/archived/meal-planning)
- [x] **shopping-list-generation** - Auto-generate shopping lists → [archived](.kiro/specs/archived/shopping-list-generation)

## Phase 3: Polish & Optimization
- [ ] **error-handling-and-validation** - Comprehensive error handling (In Progress)
- [ ] **performance-optimization** - App performance improvements (Pending)
- [ ] **accessibility-enhancements** - WCAG 2.1 Level AA compliance (Pending)
```

## Benefits

### Organizational Benefits
- **Cleaner workspace** - Only active specs visible in main directory
- **Reduced clutter** - Easier to find current work
- **Clear separation** - Completed vs. in-progress specs
- **Faster navigation** - Less scrolling through directories

### Historical Benefits
- **Preserved history** - All completed work remains accessible
- **Reference material** - Can review past decisions and implementations
- **Audit trail** - Complete record of project evolution
- **Learning resource** - New team members can study completed specs

### Productivity Benefits
- **Clear progress** - Easy to see what's done vs. in progress
- **Focus** - Attention on active work, not completed specs
- **Quick access** - Archive links in tracker for instant reference
- **Reduced confusion** - No mixing of old and new work

### Maintenance Benefits
- **Easy cleanup** - Can bulk-delete very old archives if needed
- **Backup friendly** - Archived specs can be backed up separately
- **Version control** - Git history preserved for all archived specs
- **Scalability** - Project can grow without directory bloat

## Example Workflow

### Scenario 1: Starting a New Spec

```bash
# User says: "Start the error-handling-and-validation spec"

# 1. Check tracker for completed specs
# Found: project-setup, meal-management, data-persistence, meal-planning, shopping-list-generation

# 2. Archive each completed spec
mv .kiro/specs/project-setup .kiro/specs/archived/
mv .kiro/specs/meal-management .kiro/specs/archived/
mv .kiro/specs/data-persistence .kiro/specs/archived/
mv .kiro/specs/meal-planning .kiro/specs/archived/
mv .kiro/specs/shopping-list-generation .kiro/specs/archived/

# 3. Update tracker with archive links
# Add → [archived](.kiro/specs/archived/spec-name) to each completed item

# 4. Begin work on error-handling-and-validation spec
```

### Scenario 2: Explicit Archive Request

```bash
# User says: "Archive all completed specs"

# 1. Scan .kiro/specs/ for completed specs
# 2. Move each to .kiro/specs/archived/
# 3. Update tracker
# 4. Confirm: "Archived 5 completed specs"
```

### Scenario 3: Selective Archiving

```bash
# User says: "Archive the meal-management spec"

# 1. Verify spec is completed (check tracker)
# 2. Move to archive
mv .kiro/specs/meal-management .kiro/specs/archived/

# 3. Update tracker entry
# 4. Confirm: "Archived meal-management spec"
```

### Scenario 4: Project Cleanup

```bash
# User says: "Clean up the project"

# 1. Archive all completed specs
# 2. Remove any temporary files
# 3. Update tracker
# 4. Provide summary:
#    - Archived: 5 specs
#    - Active: 2 specs
#    - Pending: 3 specs
```

## Troubleshooting

### Issue: Spec Not Found After Archiving

**Problem:** Can't find archived spec

**Solution:**
```bash
# Check archive directory
ls .kiro/specs/archived/

# Search for spec
find .kiro/specs/archived/ -name "*meal-management*"

# Check tracker for archive link
grep "meal-management" .kiro/specs/shoppit-specs-tracker.md
```

### Issue: Accidentally Archived Active Spec

**Problem:** Moved wrong spec to archive

**Solution:**
```bash
# Move back to main specs directory
mv .kiro/specs/archived/active-spec .kiro/specs/

# Update tracker (remove archive link)
# Change: [x] spec → [archived](...)
# Back to: [ ] spec (if incomplete) or [x] spec (if complete)
```

### Issue: Broken Archive Links in Tracker

**Problem:** Links in tracker don't work

**Solution:**
```bash
# Verify archive directory exists
ls .kiro/specs/archived/

# Check link format (should be relative path)
# Correct: [archived](.kiro/specs/archived/spec-name)
# Wrong: [archived](archived/spec-name)
# Wrong: [archived](/absolute/path/to/archived/spec-name)

# Update tracker with correct relative paths
```

### Issue: Archive Directory Doesn't Exist

**Problem:** `.kiro/specs/archived/` not found

**Solution:**
```bash
# Create archive directory
mkdir -p .kiro/specs/archived

# Then proceed with archiving
mv .kiro/specs/completed-spec .kiro/specs/archived/
```

### Issue: Spec Partially Archived

**Problem:** Some files moved, others left behind

**Solution:**
```bash
# Move entire directory at once
mv .kiro/specs/spec-name .kiro/specs/archived/

# If files scattered, collect them first
mkdir .kiro/specs/spec-name
mv scattered-file1.md .kiro/specs/spec-name/
mv scattered-file2.md .kiro/specs/spec-name/
mv .kiro/specs/spec-name .kiro/specs/archived/
```

### Issue: Can't Determine If Spec Is Complete

**Problem:** Unclear if spec should be archived

**Solution:**
```bash
# Check tracker status
grep "spec-name" .kiro/specs/shoppit-specs-tracker.md
# Look for [x] (complete) or [ ] (incomplete)

# Check tasks.md
cat .kiro/specs/spec-name/tasks.md
# Count completed vs. total tasks

# Check Git history
git log --oneline .kiro/specs/spec-name/
# Look for recent activity

# When in doubt, ask the user
```

## Notes

### Best Practices
- **Never delete completed specs** - always archive them
- **Maintain directory structure** - don't flatten or reorganize archived specs
- **Update tracker immediately** - don't leave broken references
- **Archive is permanent** - specs should not move back from archive
- **Archive atomically** - move entire directory at once, not file-by-file
- **Verify before archiving** - confirm spec is truly complete
- **Keep archive organized** - don't create subdirectories within archived/

### Common Mistakes to Avoid
- ❌ Archiving incomplete specs
- ❌ Deleting specs instead of archiving
- ❌ Moving individual files instead of entire directory
- ❌ Forgetting to update tracker
- ❌ Using absolute paths in archive links
- ❌ Creating nested archive directories
- ❌ Archiving specs with pending PRs

### Archive Maintenance
- **Review quarterly** - Check if very old archives can be removed
- **Backup regularly** - Include archived specs in backups
- **Document decisions** - Note why specs were archived in tracker
- **Preserve Git history** - Don't squash commits for archived specs

### Recovery Procedures

If you need to reference an archived spec:

```bash
# View archived spec
cat .kiro/specs/archived/meal-management/design.md

# Copy archived spec for reference (don't move)
cp -r .kiro/specs/archived/meal-management /tmp/reference

# Search across all archived specs
grep -r "search term" .kiro/specs/archived/
```

If you need to reopen an archived spec:

```bash
# This should be rare - usually create a new spec instead
# But if necessary:
mv .kiro/specs/archived/spec-name .kiro/specs/

# Update tracker (remove archive link, mark as active)
# Consider: Is this a new spec or continuation?
```
