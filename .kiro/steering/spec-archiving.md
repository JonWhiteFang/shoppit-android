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

Archive completed specs:
- **Before starting a new spec** from the tracker
- **When explicitly requested** by the user
- **During project cleanup** or reorganization

## Archive Structure

```
.kiro/specs/
├── archived/
│   ├── project-setup/          # Completed Phase 1
│   ├── meal-management/        # Completed Phase 1
│   ├── data-persistence/       # Completed Phase 1
│   ├── meal-planning/          # Completed Phase 2
│   └── shopping-list-generation/ # Completed Phase 2
├── error-handling-and-validation/  # Active/Incomplete
├── performance-optimization/       # Active/Incomplete
└── shoppit-specs-tracker.md
```

## Benefits

- **Cleaner workspace** - Only active specs visible in main directory
- **Preserved history** - All completed work remains accessible
- **Clear progress** - Easy to see what's done vs. in progress
- **Easy reference** - Archive links in tracker for quick access

## Example Workflow

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

# 4. Begin work on error-handling-and-validation spec
```

## Notes

- **Never delete completed specs** - always archive them
- **Maintain directory structure** - don't flatten or reorganize archived specs
- **Update tracker immediately** - don't leave broken references
- **Archive is permanent** - specs should not move back from archive
