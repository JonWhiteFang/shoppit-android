---
inclusion: always
---

# Git Workflow

## Branch Strategy

### Main Branches
- **main**: Production-ready code, always deployable
- **develop**: Integration branch for features, next release candidate

### Supporting Branches
- **feature/**: New features and enhancements
- **bugfix/**: Bug fixes for develop branch
- **hotfix/**: Urgent fixes for production
- **release/**: Release preparation

## Branch Naming Conventions

### Feature Branches
```
feature/<issue-number>-<short-description>

Examples:
feature/123-meal-planner-ui
feature/456-shopping-list-aggregation
feature/789-add-meal-categories
feature/101-implement-search
```

**When to use:** New features, enhancements, or any additive changes.

### Bugfix Branches
```
bugfix/<issue-number>-<short-description>

Examples:
bugfix/789-fix-ingredient-duplication
bugfix/101-crash-on-empty-meal
bugfix/234-incorrect-quantity-calculation
bugfix/567-navigation-back-stack-issue
```

**When to use:** Fixing bugs in the develop branch that aren't critical enough for hotfix.

### Hotfix Branches
```
hotfix/<version>-<short-description>

Examples:
hotfix/1.2.1-critical-data-loss
hotfix/1.3.2-security-vulnerability
hotfix/2.0.1-app-crash-on-launch
```

**When to use:** Critical bugs in production that need immediate fix.

### Release Branches
```
release/<version>

Examples:
release/1.3.0
release/2.0.0
release/1.4.0-rc1
```

**When to use:** Preparing a new production release, final testing and bug fixes only.

## Commit Message Format

Follow Conventional Commits specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, no logic change)
- **refactor**: Code refactoring (no feature change or bug fix)
- **perf**: Performance improvements
- **test**: Adding or updating tests
- **build**: Build system or dependency changes
- **ci**: CI/CD configuration changes
- **chore**: Other changes (tooling, etc.)

### Scope (Optional)
- **meal**: Meal management feature
- **planner**: Meal planner feature
- **shopping**: Shopping list feature
- **data**: Data layer changes
- **ui**: UI layer changes
- **domain**: Domain layer changes

### Examples

```bash
# Feature
feat(meal): add ingredient quantity validation

Implement validation to ensure ingredient quantities are positive numbers.
Reject empty or negative values with appropriate error messages.

Closes #123

# Bug fix
fix(shopping): prevent duplicate ingredients in aggregation

Fixed issue where ingredients with same name but different casing
were not being aggregated correctly.

Fixes #456

# Refactor
refactor(data): extract mapper functions to separate file

Moved all entity-to-model mapping functions to dedicated mapper file
for better organization and reusability.

# Documentation
docs: update README with setup instructions

Added detailed setup instructions including Gradle version requirements
and Android SDK configuration.

# Test
test(meal): add unit tests for MealViewModel

Added comprehensive unit tests covering loading, error, and success states.
```

## Workflow Steps

### Starting New Feature

```bash
# Update develop branch
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/123-meal-planner-ui

# Work on feature, commit regularly
git add .
git commit -m "feat(planner): add meal plan calendar view"

# Push to remote
git push -u origin feature/123-meal-planner-ui
```

### Keeping Feature Branch Updated

```bash
# Fetch latest changes
git fetch origin

# Rebase on develop (preferred for clean history)
git checkout feature/123-meal-planner-ui
git rebase origin/develop

# Or merge if rebase is problematic
git merge origin/develop

# Push (force push if rebased)
git push --force-with-lease
```

### Completing Feature

```bash
# Ensure branch is up to date
git checkout feature/123-meal-planner-ui
git rebase origin/develop

# Push final changes
git push --force-with-lease

# Create Pull Request on GitHub
# After PR approval and CI passes, merge via GitHub UI
```

### Hotfix Workflow

```bash
# Create hotfix from main
git checkout main
git pull origin main
git checkout -b hotfix/1.2.1-critical-data-loss

# Fix the issue
git add .
git commit -m "fix: prevent data loss on app background"

# Push and create PR to main
git push -u origin hotfix/1.2.1-critical-data-loss

# After merging to main, also merge to develop
git checkout develop
git merge hotfix/1.2.1-critical-data-loss
git push origin develop
```

## Pull Request Guidelines

### PR Title Format
```
[Type] Short description (#issue-number)

feat: Add meal planner calendar view (#123)
fix: Prevent duplicate ingredients in shopping list (#456)
refactor: Extract repository mappers to separate files
```

### PR Description Template

```markdown
## Description
Brief description of changes and motivation.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Refactoring (no functional changes)
- [ ] Documentation update

## Related Issues
Closes #123
Related to #456

## Changes Made
- Added meal plan calendar view
- Implemented date selection logic
- Created MealPlanViewModel with state management
- Added unit tests for ViewModel

## Testing
- [ ] Unit tests added/updated
- [ ] Instrumented tests added/updated
- [ ] Manual testing completed
- [ ] All tests passing

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No new warnings introduced
- [ ] Tests added/updated and passing
- [ ] Branch is up to date with develop
```

### PR Review Process

**Before Requesting Review:**
- All tests pass locally
- Code is self-reviewed
- Branch is up to date with develop
- Commit messages follow conventions
- No debug code or commented-out code

**Reviewer Checklist:**
- Code follows architecture patterns (Clean Architecture, MVVM)
- Proper dependency injection with Hilt
- State management follows guidelines
- Error handling is comprehensive
- Tests are meaningful and sufficient
- No performance issues introduced
- UI follows Material3 guidelines
- Documentation is clear

## Commit Best Practices

### Atomic Commits
Each commit should represent a single logical change:

```bash
# Good - single logical change
git commit -m "feat(meal): add ingredient validation"

# Bad - multiple unrelated changes
git commit -m "feat(meal): add validation and fix UI bug and update docs"
```

### Commit Frequency
- Commit often, but keep commits meaningful
- Commit after completing a logical unit of work
- Don't commit broken code (unless using WIP commits on feature branch)

### Amending Commits
```bash
# Amend last commit (before pushing)
git add .
git commit --amend --no-edit

# Amend with new message
git commit --amend -m "feat(meal): add comprehensive ingredient validation"
```

### Interactive Rebase
Clean up commits before creating PR:

```bash
# Rebase last 3 commits
git rebase -i HEAD~3

# Options:
# pick - keep commit as is
# reword - change commit message
# squash - combine with previous commit
# fixup - like squash but discard commit message
# drop - remove commit
```

## Merge Strategies

### Squash and Merge (Preferred for Features)
**What it does:** Combines all feature commits into single commit on develop

**Pros:**
- Keeps develop history clean and readable
- Each feature = one commit
- Easy to revert entire feature

**Cons:**
- Loses individual commit history
- Can't cherry-pick specific commits from feature

**Use for:**
- Feature branches with many small commits
- Experimental work with messy commit history
- Features developed by single developer

**Example:**
```bash
# On GitHub PR, select "Squash and merge"
# Or via command line:
git checkout develop
git merge --squash feature/123-meal-planner
git commit -m "feat(planner): add meal plan calendar view (#123)"
```

### Rebase and Merge
**What it does:** Replays feature commits on top of develop, maintains individual commits

**Pros:**
- Linear history (no merge commits)
- Preserves individual commits
- Can cherry-pick specific commits

**Cons:**
- Requires clean, well-organized commits
- More complex for beginners

**Use for:**
- Small, well-organized branches (2-5 commits)
- Commits that tell a clear story
- When individual commits are valuable

**Example:**
```bash
# Update feature branch first
git checkout feature/123-meal-planner
git rebase origin/develop
git push --force-with-lease

# On GitHub PR, select "Rebase and merge"
# Or via command line:
git checkout develop
git rebase feature/123-meal-planner
```

### Merge Commit (Standard Merge)
**What it does:** Creates a merge commit that joins two branches

**Pros:**
- Preserves complete history
- Shows when feature was integrated
- Safe and reversible

**Cons:**
- Creates merge commits (clutters history)
- Non-linear history

**Use for:**
- Release branches merging to main
- Hotfix branches
- Long-running feature branches with multiple contributors

**Example:**
```bash
git checkout develop
git merge --no-ff feature/123-meal-planner
git push origin develop
```

### Decision Tree
```
What are you merging?
├─ Feature branch with messy commits? → Squash and Merge
├─ Small branch with clean commits? → Rebase and Merge
├─ Release or hotfix branch? → Merge Commit
└─ Not sure? → Squash and Merge (safest default)
```

## Git Hooks

### Pre-commit Hook
Create `.git/hooks/pre-commit`:

```bash
#!/bin/sh

# Run ktlint
./gradlew ktlintCheck

# Run unit tests
./gradlew test

if [ $? -ne 0 ]; then
    echo "Tests failed. Commit aborted."
    exit 1
fi
```

### Commit Message Hook
Create `.git/hooks/commit-msg`:

```bash
#!/bin/sh

# Validate commit message format
commit_msg=$(cat "$1")
pattern="^(feat|fix|docs|style|refactor|perf|test|build|ci|chore)(\(.+\))?: .{1,50}"

if ! echo "$commit_msg" | grep -qE "$pattern"; then
    echo "Invalid commit message format."
    echo "Use: <type>(<scope>): <subject>"
    exit 1
fi
```

## Useful Git Commands

### Viewing History
```bash
# Compact log
git log --oneline --graph --decorate

# Show changes in commit
git show <commit-hash>

# Search commits
git log --grep="meal planner"

# Show file history
git log --follow -- path/to/file.kt
```

### Undoing Changes
```bash
# Discard local changes
git checkout -- file.kt

# Unstage file
git reset HEAD file.kt

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes)
git reset --hard HEAD~1

# Revert commit (creates new commit)
git revert <commit-hash>
```

### Stashing
```bash
# Stash changes
git stash

# Stash with message
git stash save "WIP: meal planner feature"

# List stashes
git stash list

# Apply stash
git stash apply

# Apply and remove stash
git stash pop

# Drop stash
git stash drop stash@{0}
```

### Cherry-picking
```bash
# Apply specific commit to current branch
git cherry-pick <commit-hash>

# Cherry-pick without committing
git cherry-pick -n <commit-hash>

# Cherry-pick a range of commits
git cherry-pick <start-hash>^..<end-hash>

# Cherry-pick and edit commit message
git cherry-pick -e <commit-hash>
```

### Comparing Changes
```bash
# Compare working directory with last commit
git diff

# Compare staged changes
git diff --staged

# Compare two branches
git diff develop..feature/123-my-feature

# Compare specific file between branches
git diff develop..feature/123-my-feature -- path/to/file.kt

# Show changes in a commit
git show <commit-hash>

# Show files changed in a commit
git show --name-only <commit-hash>
```

### Finding Bugs with Git Bisect
```bash
# Start bisect session
git bisect start

# Mark current commit as bad
git bisect bad

# Mark a known good commit
git bisect good <commit-hash>

# Git will checkout commits for you to test
# After testing each:
git bisect good  # if bug not present
git bisect bad   # if bug present

# Git will identify the problematic commit
# End bisect session
git bisect reset
```

### Cleaning Up
```bash
# Remove untracked files (dry run first)
git clean -n

# Remove untracked files
git clean -f

# Remove untracked files and directories
git clean -fd

# Remove ignored files too
git clean -fdx

# Delete local branches that are merged
git branch --merged | grep -v "\*" | xargs -n 1 git branch -d

# Delete remote-tracking branches that no longer exist
git fetch --prune
```

## .gitignore

Ensure `.gitignore` includes:

```gitignore
# Android
*.apk
*.ap_
*.aab
*.dex
*.class
bin/
gen/
out/
build/
.gradle/
local.properties

# IDE
.idea/
*.iml
.DS_Store

# Keystore files
*.jks
*.keystore

# External native build
.externalNativeBuild
.cxx/

# Version control
*.orig
*.rej
```

## Troubleshooting

### Merge Conflicts

**Scenario:** You're rebasing or merging and encounter conflicts.

```bash
# View conflicted files
git status

# Edit files to resolve conflicts
# Look for conflict markers:
# <<<<<<< HEAD (your changes)
# =======
# >>>>>>> branch-name (incoming changes)

# After resolving, mark as resolved
git add resolved-file.kt

# Continue merge/rebase
git merge --continue
# or
git rebase --continue

# If you want to abort
git merge --abort
# or
git rebase --abort
```

**Tips:**
- Use a merge tool: `git mergetool`
- Keep conflicts small by rebasing frequently
- Communicate with team about overlapping work

### Accidentally Committed to Wrong Branch

**Scenario:** You committed to `develop` instead of a feature branch.

```bash
# On wrong branch (develop)
git reset --soft HEAD~1

# Switch to correct branch (create if needed)
git checkout -b feature/123-my-feature

# Commit changes
git add .
git commit -m "feat: correct commit message"

# Push to remote
git push -u origin feature/123-my-feature
```

**Alternative:** Move commits to existing branch
```bash
# On wrong branch, note the commit hash
git log --oneline -n 1

# Switch to correct branch
git checkout feature/123-my-feature

# Cherry-pick the commit
git cherry-pick <commit-hash>

# Go back and remove from wrong branch
git checkout develop
git reset --hard HEAD~1
```

### Accidentally Pushed to Wrong Branch

**Scenario:** You pushed commits to the wrong remote branch.

```bash
# If no one else has pulled your changes:
# Reset local branch
git reset --hard HEAD~1

# Force push to remove from remote
git push --force-with-lease

# Then push to correct branch
git checkout correct-branch
git cherry-pick <commit-hash>
git push
```

**If others have pulled:** Don't force push! Use `git revert` instead.

### Rebase Conflicts (Multiple)

**Scenario:** You have many conflicts during rebase.

```bash
# If rebase is too painful, abort and use merge instead
git rebase --abort
git merge origin/develop

# Or, skip problematic commits (use with caution)
git rebase --skip

# Or, continue after resolving each conflict
git add .
git rebase --continue
```

### Lost Commits (Reflog to the Rescue)

**Scenario:** You accidentally reset or deleted commits.

```bash
# View reflog (history of HEAD movements)
git reflog

# Find the commit you want to recover
# Look for entries like: HEAD@{2}: commit: feat(meal): add validation

# Reset to that commit
git reset --hard HEAD@{2}

# Or cherry-pick specific commit
git cherry-pick <commit-hash-from-reflog>
```

### Detached HEAD State

**Scenario:** You checked out a specific commit and are in "detached HEAD" state.

```bash
# If you made changes you want to keep:
git checkout -b new-branch-name

# If you want to discard changes:
git checkout develop
```

### Large File Accidentally Committed

**Scenario:** You committed a large file that shouldn't be in the repo.

```bash
# Remove from last commit (before pushing)
git rm --cached large-file.apk
git commit --amend --no-edit

# If already pushed, use BFG Repo-Cleaner or git filter-branch
# (Complex - consult team first)
```

### Forgot to Pull Before Starting Work

**Scenario:** You made commits but forgot to pull latest changes first.

```bash
# Stash your changes
git stash

# Pull latest changes
git pull origin develop

# Reapply your changes
git stash pop

# If conflicts, resolve them
# Then commit your work
```

### Pushed Sensitive Data (API Keys, Passwords)

**Scenario:** You accidentally committed and pushed sensitive information.

**Immediate actions:**
1. **Rotate the credentials immediately** (change passwords, regenerate API keys)
2. Remove from history using `git filter-branch` or BFG Repo-Cleaner
3. Force push to rewrite history
4. Notify team members to re-clone repository

```bash
# Remove file from all history (use BFG Repo-Cleaner)
bfg --delete-files sensitive-file.txt
git reflog expire --expire=now --all
git gc --prune=now --aggressive
git push --force
```

**Prevention:** Use `.gitignore` and pre-commit hooks.

### Force Push Safety

```bash
# NEVER use git push --force
# ALWAYS use --force-with-lease
git push --force-with-lease

# This fails if remote has commits you don't have locally
# Protects against overwriting others' work
```

### Undo Last Push (Not Recommended)

**Scenario:** You pushed something wrong and need to undo it.

```bash
# Only if no one else has pulled!
git reset --hard HEAD~1
git push --force-with-lease

# If others have pulled, use revert instead:
git revert HEAD
git push
```

### Branch Diverged from Remote

**Scenario:** `git status` shows "Your branch and 'origin/feature' have diverged".

```bash
# Option 1: Rebase your changes on top of remote
git pull --rebase origin feature/123-my-feature

# Option 2: Merge remote changes into your branch
git pull origin feature/123-my-feature

# Option 3: Force push your version (if you're sure)
git push --force-with-lease
```

## Common Scenarios

### Scenario 1: Starting a New Feature
```bash
# 1. Ensure develop is up to date
git checkout develop
git pull origin develop

# 2. Create feature branch
git checkout -b feature/123-meal-categories

# 3. Make changes and commit
git add .
git commit -m "feat(meal): add category model and database schema"

# 4. Push to remote
git push -u origin feature/123-meal-categories

# 5. Continue working, commit regularly
git add .
git commit -m "feat(meal): implement category selection UI"
git push

# 6. Keep branch updated with develop
git fetch origin
git rebase origin/develop
git push --force-with-lease

# 7. Create PR when ready
```

### Scenario 2: Fixing a Bug in Production
```bash
# 1. Create hotfix from main
git checkout main
git pull origin main
git checkout -b hotfix/1.2.1-fix-crash

# 2. Fix the bug
git add .
git commit -m "fix: prevent crash when meal has no ingredients"

# 3. Push and create PR to main
git push -u origin hotfix/1.2.1-fix-crash

# 4. After merging to main, merge to develop
git checkout develop
git pull origin develop
git merge hotfix/1.2.1-fix-crash
git push origin develop

# 5. Delete hotfix branch
git branch -d hotfix/1.2.1-fix-crash
git push origin --delete hotfix/1.2.1-fix-crash
```

### Scenario 3: Updating Feature Branch with Latest Develop
```bash
# Option 1: Rebase (preferred - cleaner history)
git checkout feature/123-my-feature
git fetch origin
git rebase origin/develop
# Resolve conflicts if any
git push --force-with-lease

# Option 2: Merge (safer if unsure)
git checkout feature/123-my-feature
git pull origin develop
git push
```

### Scenario 4: Splitting a Large Commit
```bash
# Reset last commit but keep changes
git reset --soft HEAD~1

# Stage and commit changes in smaller chunks
git add file1.kt file2.kt
git commit -m "feat(meal): add category model"

git add file3.kt file4.kt
git commit -m "feat(meal): implement category UI"
```

### Scenario 5: Reviewing Someone's PR Locally
```bash
# Fetch PR branch
git fetch origin pull/123/head:pr-123

# Checkout PR branch
git checkout pr-123

# Test the changes
.\gradlew.bat test

# Return to your branch
git checkout feature/my-feature

# Delete PR branch when done
git branch -D pr-123
```

## Quick Reference

### Branch Naming
- `feature/123-short-description` - New features
- `bugfix/456-short-description` - Bug fixes
- `hotfix/1.2.1-short-description` - Production fixes
- `release/1.3.0` - Release preparation

### Commit Types
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation
- `refactor` - Code refactoring
- `test` - Tests
- `chore` - Maintenance
- `perf` - Performance improvements
- `style` - Code style changes
- `build` - Build system changes
- `ci` - CI/CD changes

### Common Scopes
- `meal`, `planner`, `shopping` - Features
- `data`, `domain`, `ui` - Layers

### Essential Commands Cheat Sheet
```bash
# Status and info
git status                          # Check current state
git log --oneline --graph          # View commit history
git branch -a                       # List all branches

# Making changes
git add .                           # Stage all changes
git commit -m "type: message"      # Commit with message
git push                            # Push to remote

# Branching
git checkout -b feature/123-name   # Create and switch to branch
git checkout develop               # Switch to branch
git branch -d feature/123-name     # Delete local branch

# Updating
git pull origin develop            # Pull latest from develop
git fetch origin                   # Fetch without merging
git rebase origin/develop          # Rebase on develop

# Undoing
git reset --soft HEAD~1            # Undo commit, keep changes
git reset --hard HEAD~1            # Undo commit, discard changes
git checkout -- file.kt            # Discard file changes
git stash                          # Temporarily save changes

# Troubleshooting
git reflog                         # View all HEAD movements
git clean -fd                      # Remove untracked files
git push --force-with-lease        # Safe force push
```

### Windows-Specific Notes
- Use `.\ gradlew.bat` instead of `./gradlew`
- Use backslashes `\` for paths in commands
- PowerShell: Use `;` to chain commands
- CMD: Use `&` to chain commands
- Line endings: Git auto-converts CRLF ↔ LF
