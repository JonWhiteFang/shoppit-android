# Git Workflow Guide

This guide covers the Git workflow, branching strategy, commit conventions, and pull request process for the Shoppit project.

## Overview

Shoppit uses a Git Flow-inspired workflow with two main branches and supporting feature branches. This approach provides:

- **Stable main branch** - Always production-ready
- **Active development** - Continuous integration on develop
- **Isolated features** - Work on features without affecting others
- **Clear history** - Conventional commits and clean merge strategy

## Branch Strategy

### Main Branches

#### main
- **Purpose**: Production-ready code, always deployable
- **Protection**: Protected branch, requires PR approval
- **Merges from**: Release branches, hotfix branches
- **Never commit directly** to main

#### develop
- **Purpose**: Integration branch for features, next release candidate
- **Protection**: Protected branch, requires PR approval
- **Merges from**: Feature branches, bugfix branches
- **Base for**: New feature branches

### Supporting Branches

#### feature/
- **Purpose**: New features and enhancements
- **Base**: develop
- **Merge to**: develop
- **Lifetime**: Until feature is complete
- **Naming**: `feature/<issue-number>-<short-description>`

#### bugfix/
- **Purpose**: Bug fixes for develop branch
- **Base**: develop
- **Merge to**: develop
- **Lifetime**: Until bug is fixed
- **Naming**: `bugfix/<issue-number>-<short-description>`

#### hotfix/
- **Purpose**: Urgent fixes for production
- **Base**: main
- **Merge to**: main AND develop
- **Lifetime**: Until fix is deployed
- **Naming**: `hotfix/<version>-<short-description>`

#### release/
- **Purpose**: Release preparation (version bump, final testing)
- **Base**: develop
- **Merge to**: main AND develop
- **Lifetime**: Until release is deployed
- **Naming**: `release/<version>`

## Branch Naming Conventions

Use descriptive, kebab-case names with issue numbers:

### Feature Branches
```
feature/<issue-number>-<short-description>

Examples:
feature/123-meal-planner-ui
feature/456-shopping-list-aggregation
feature/789-offline-sync
```

### Bugfix Branches
```
bugfix/<issue-number>-<short-description>

Examples:
bugfix/234-fix-ingredient-duplication
bugfix/567-crash-on-empty-meal
bugfix/890-memory-leak-viewmodel
```

### Hotfix Branches
```
hotfix/<version>-<short-description>

Examples:
hotfix/1.2.1-critical-data-loss
hotfix/1.3.2-crash-on-startup
```

### Release Branches
```
release/<version>

Examples:
release/1.3.0
release/2.0.0
```

## Commit Message Format

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Commit Types

- **feat**: New feature for the user
- **fix**: Bug fix for the user
- **docs**: Documentation changes
- **style**: Code style changes (formatting, missing semicolons, etc.)
- **refactor**: Code refactoring (no feature change or bug fix)
- **perf**: Performance improvements
- **test**: Adding or updating tests
- **build**: Build system or dependency changes
- **ci**: CI/CD configuration changes
- **chore**: Other changes (tooling, configuration, etc.)

### Commit Scopes

Use feature or layer names as scopes:

- **meal**: Meal management feature
- **planner**: Meal planner feature
- **shopping**: Shopping list feature
- **data**: Data layer changes
- **domain**: Domain layer changes
- **ui**: UI layer changes
- **di**: Dependency injection changes

### Commit Examples

```bash
# Feature with body and footer
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

# Performance
perf(shopping): optimize ingredient aggregation algorithm

Reduced time complexity from O(n²) to O(n) by using HashMap for lookups.

# Build
build: update Kotlin to 2.0.21

Updated Kotlin version and related dependencies to latest stable release.
```

### Commit Message Rules

1. **Subject line** (first line):
   - Max 50 characters
   - Start with lowercase (after type)
   - No period at the end
   - Imperative mood ("add" not "added" or "adds")

2. **Body** (optional):
   - Wrap at 72 characters
   - Explain what and why, not how
   - Separate from subject with blank line

3. **Footer** (optional):
   - Reference issues: `Closes #123`, `Fixes #456`, `Related to #789`
   - Breaking changes: `BREAKING CHANGE: description`

## Workflow Steps

### Starting a New Feature

```bash
# 1. Update develop branch
git checkout develop
git pull origin develop

# 2. Create feature branch
git checkout -b feature/123-meal-planner-ui

# 3. Work on feature, commit regularly
git add .
git commit -m "feat(planner): add meal plan calendar view"

# 4. Continue working
git add .
git commit -m "feat(planner): implement date selection logic"

# 5. Push to remote
git push -u origin feature/123-meal-planner-ui
```

### Keeping Feature Branch Updated

Keep your feature branch up to date with develop to avoid merge conflicts:

```bash
# 1. Fetch latest changes
git fetch origin

# 2. Option A: Rebase (preferred for clean history)
git checkout feature/123-meal-planner-ui
git rebase origin/develop

# If conflicts occur during rebase:
# - Resolve conflicts in files
# - git add <resolved-files>
# - git rebase --continue

# 3. Option B: Merge (if rebase is problematic)
git checkout feature/123-meal-planner-ui
git merge origin/develop

# 4. Push changes (force push if rebased)
git push --force-with-lease
```

**When to rebase vs merge:**
- **Rebase**: For feature branches to keep linear history
- **Merge**: If branch is shared with others or rebase causes issues

### Completing a Feature

```bash
# 1. Ensure branch is up to date
git checkout feature/123-meal-planner-ui
git rebase origin/develop

# 2. Clean up commits (optional but recommended)
git rebase -i origin/develop

# 3. Push final changes
git push --force-with-lease

# 4. Create Pull Request on GitHub
# - Go to repository on GitHub
# - Click "New Pull Request"
# - Select feature/123-meal-planner-ui → develop
# - Fill in PR template
# - Request reviewers

# 5. After PR approval and CI passes
# - Merge via GitHub UI (squash and merge)
# - Delete feature branch
```

### Hotfix Workflow

For urgent production fixes:

```bash
# 1. Create hotfix from main
git checkout main
git pull origin main
git checkout -b hotfix/1.2.1-critical-data-loss

# 2. Fix the issue
git add .
git commit -m "fix: prevent data loss on app background"

# 3. Push and create PR to main
git push -u origin hotfix/1.2.1-critical-data-loss

# 4. Create PR to main, get approval, merge

# 5. Also merge hotfix to develop
git checkout develop
git pull origin develop
git merge hotfix/1.2.1-critical-data-loss
git push origin develop

# 6. Tag the release
git checkout main
git pull origin main
git tag -a v1.2.1 -m "Hotfix: prevent data loss"
git push origin v1.2.1
```

### Release Workflow

For preparing releases:

```bash
# 1. Create release branch from develop
git checkout develop
git pull origin develop
git checkout -b release/1.3.0

# 2. Update version numbers
# - Update version in build.gradle.kts
# - Update CHANGELOG.md

git add .
git commit -m "chore: bump version to 1.3.0"

# 3. Push and create PR to main
git push -u origin release/1.3.0

# 4. After testing and approval, merge to main
# 5. Tag the release
git checkout main
git pull origin main
git tag -a v1.3.0 -m "Release 1.3.0"
git push origin v1.3.0

# 6. Merge back to develop
git checkout develop
git merge release/1.3.0
git push origin develop

# 7. Delete release branch
git branch -d release/1.3.0
git push origin --delete release/1.3.0
```

## Pull Request Guidelines

### PR Title Format

```
[Type] Short description (#issue-number)

Examples:
feat: Add meal planner calendar view (#123)
fix: Prevent duplicate ingredients in shopping list (#456)
refactor: Extract repository mappers to separate files
docs: Update testing guide with new patterns
```

### PR Description Template

Use this template for all pull requests:

```markdown
## Description
Brief description of changes and motivation.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Refactoring (no functional changes)
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Test improvements

## Related Issues
Closes #123
Related to #456

## Changes Made
- Added meal plan calendar view with date selection
- Implemented MealPlanViewModel with state management
- Created MealPlanUiState sealed class for state handling
- Added unit tests for ViewModel (85% coverage)
- Updated navigation to include planner screen

## Testing
- [ ] Unit tests added/updated
- [ ] Instrumented tests added/updated
- [ ] Manual testing completed on emulator
- [ ] Manual testing completed on physical device
- [ ] All tests passing locally
- [ ] No new lint warnings

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Performance Impact
- [ ] No performance impact
- [ ] Performance improved
- [ ] Performance impact assessed and acceptable

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated (if needed)
- [ ] No debug code or commented-out code
- [ ] Tests added/updated and passing
- [ ] Branch is up to date with develop
- [ ] Commit messages follow conventions
```

### Before Requesting Review

Ensure your PR is ready:

✅ **Code Quality**
- All tests pass locally
- No lint warnings
- Code follows style guidelines
- Self-review completed

✅ **Documentation**
- Comments added for complex logic
- Documentation updated if needed
- CHANGELOG.md updated (for features/fixes)

✅ **Git Hygiene**
- Branch is up to date with develop
- Commit messages follow conventions
- No debug code or commented-out code
- No merge commits (use rebase)

✅ **Testing**
- Unit tests added for new code
- Instrumented tests for critical paths
- Manual testing completed
- All tests passing

### PR Review Process

**Reviewer Checklist:**

✅ **Architecture & Design**
- Follows Clean Architecture patterns
- Proper layer separation (UI → Domain ← Data)
- Dependency direction is correct
- No circular dependencies

✅ **Code Quality**
- Follows Kotlin conventions
- Proper naming conventions
- No code duplication
- Appropriate use of design patterns

✅ **Dependency Injection**
- Proper use of Hilt annotations
- Constructor injection used
- Appropriate scopes
- No manual dependency creation

✅ **State Management**
- ViewModels expose `StateFlow<UiState>`
- State classes are immutable
- Proper state hoisting in Compose
- No mutable state exposed

✅ **Error Handling**
- Exceptions caught at boundaries
- Errors mapped to domain errors
- User-friendly error messages
- No silent failures

✅ **Testing**
- Meaningful tests (not just coverage)
- Tests follow AAA pattern (Arrange-Act-Assert)
- Mocking is appropriate
- Tests are maintainable

✅ **Performance**
- No obvious performance issues
- Proper use of coroutines and Flow
- Database queries are efficient
- UI doesn't block main thread

✅ **UI/UX**
- Follows Material3 guidelines
- Responsive and accessible
- Proper loading and error states
- Consistent with app design

### Requesting Changes

When requesting changes:
- Be specific and constructive
- Explain why the change is needed
- Suggest alternatives if possible
- Distinguish between blocking and non-blocking comments

### Approving PRs

Before approving:
- All checklist items are satisfied
- CI/CD pipeline passes
- No unresolved conversations
- Code meets quality standards

## Commit Best Practices

### Atomic Commits

Each commit should represent a single logical change:

```bash
# Good - single logical change
git commit -m "feat(meal): add ingredient validation"

# Bad - multiple unrelated changes
git commit -m "feat(meal): add validation and fix UI bug and update docs"
```

**Benefits:**
- Easier to review
- Easier to revert if needed
- Clearer history
- Better for git bisect

### Commit Frequency

- **Commit often** - Don't wait until feature is complete
- **Commit meaningful units** - Each commit should be a logical step
- **Don't commit broken code** - Unless using WIP commits on feature branch
- **Commit before switching tasks** - Save your work

### Amending Commits

Fix the last commit before pushing:

```bash
# Add forgotten files to last commit
git add forgotten-file.kt
git commit --amend --no-edit

# Change last commit message
git commit --amend -m "feat(meal): add comprehensive ingredient validation"

# Amend and edit message in editor
git commit --amend
```

**Warning:** Never amend commits that have been pushed to shared branches!

### Interactive Rebase

Clean up commits before creating PR:

```bash
# Rebase last 3 commits
git rebase -i HEAD~3

# Or rebase from develop
git rebase -i origin/develop
```

**Interactive rebase options:**
- `pick` - Keep commit as is
- `reword` - Change commit message
- `edit` - Stop to amend commit
- `squash` - Combine with previous commit, keep both messages
- `fixup` - Combine with previous commit, discard this message
- `drop` - Remove commit

**Example:**
```
pick abc1234 feat(meal): add meal list screen
fixup def5678 fix typo
fixup ghi9012 fix lint error
reword jkl3456 feat(meal): add meal detail screen
```

## Merge Strategies

### Squash and Merge (Preferred for Features)

**When to use:** Feature branches merging to develop

**Benefits:**
- Clean, linear history on develop
- One commit per feature
- Easy to revert entire feature

**How it works:**
- All commits in PR are combined into one
- Commit message is PR title + description
- Feature branch can be deleted after merge

### Rebase and Merge

**When to use:** Small, well-organized branches

**Benefits:**
- Maintains individual commits
- Linear history
- No merge commits

**How it works:**
- Commits are rebased onto target branch
- Each commit appears in history
- Requires clean commit history

### Merge Commit

**When to use:** Release branches, hotfixes

**Benefits:**
- Preserves full branch history
- Shows when branches were merged
- Easy to see feature boundaries

**How it works:**
- Creates a merge commit
- Both branch histories preserved
- Non-linear history

## Git Hooks

Automate checks before commits and pushes.

### Pre-commit Hook

Create `.git/hooks/pre-commit`:

```bash
#!/bin/sh

echo "Running pre-commit checks..."

# Run ktlint
echo "Checking code style..."
./gradlew ktlintCheck
if [ $? -ne 0 ]; then
    echo "❌ Code style check failed. Run ./gradlew ktlintFormat to fix."
    exit 1
fi

# Run unit tests
echo "Running unit tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "❌ Tests failed. Fix failing tests before committing."
    exit 1
fi

echo "✅ Pre-commit checks passed!"
exit 0
```

Make it executable:
```bash
chmod +x .git/hooks/pre-commit
```

### Commit Message Hook

Create `.git/hooks/commit-msg`:

```bash
#!/bin/sh

# Validate commit message format
commit_msg=$(cat "$1")
pattern="^(feat|fix|docs|style|refactor|perf|test|build|ci|chore)(\(.+\))?: .{1,50}"

if ! echo "$commit_msg" | grep -qE "$pattern"; then
    echo "❌ Invalid commit message format."
    echo "Format: <type>(<scope>): <subject>"
    echo "Example: feat(meal): add ingredient validation"
    exit 1
fi

echo "✅ Commit message format valid"
exit 0
```

Make it executable:
```bash
chmod +x .git/hooks/commit-msg
```

### Pre-push Hook

Create `.git/hooks/pre-push`:

```bash
#!/bin/sh

echo "Running pre-push checks..."

# Run all tests
echo "Running all tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "❌ Tests failed. Fix failing tests before pushing."
    exit 1
fi

echo "✅ Pre-push checks passed!"
exit 0
```

## Useful Git Commands

### Viewing History

```bash
# Compact log with graph
git log --oneline --graph --decorate --all

# Show changes in specific commit
git show <commit-hash>

# Show changes in file
git show <commit-hash>:path/to/file.kt

# Search commits by message
git log --grep="meal planner"

# Search commits by author
git log --author="John Doe"

# Show file history
git log --follow -- path/to/file.kt

# Show commits that changed specific line
git log -L 10,20:path/to/file.kt
```

### Undoing Changes

```bash
# Discard local changes in file
git checkout -- file.kt

# Discard all local changes
git checkout -- .

# Unstage file (keep changes)
git reset HEAD file.kt

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes)
git reset --hard HEAD~1

# Undo last 3 commits (keep changes)
git reset --soft HEAD~3

# Revert commit (creates new commit)
git revert <commit-hash>

# Revert merge commit
git revert -m 1 <merge-commit-hash>
```

### Stashing

```bash
# Stash changes
git stash

# Stash with message
git stash save "WIP: meal planner feature"

# Stash including untracked files
git stash -u

# List stashes
git stash list

# Show stash contents
git stash show stash@{0}

# Apply stash (keep in stash list)
git stash apply

# Apply specific stash
git stash apply stash@{2}

# Apply and remove stash
git stash pop

# Drop stash
git stash drop stash@{0}

# Clear all stashes
git stash clear
```

### Cherry-picking

```bash
# Apply specific commit to current branch
git cherry-pick <commit-hash>

# Cherry-pick without committing
git cherry-pick -n <commit-hash>

# Cherry-pick range of commits
git cherry-pick <start-hash>..<end-hash>

# Cherry-pick and edit message
git cherry-pick -e <commit-hash>
```

### Branch Management

```bash
# List all branches
git branch -a

# List remote branches
git branch -r

# Delete local branch
git branch -d feature/123-meal-planner-ui

# Force delete local branch
git branch -D feature/123-meal-planner-ui

# Delete remote branch
git push origin --delete feature/123-meal-planner-ui

# Rename current branch
git branch -m new-branch-name

# Show branches merged to current
git branch --merged

# Show branches not merged to current
git branch --no-merged
```

## Troubleshooting

### Merge Conflicts

```bash
# 1. View conflicted files
git status

# 2. Open files and resolve conflicts
# Look for conflict markers:
# <<<<<<< HEAD
# Your changes
# =======
# Their changes
# >>>>>>> branch-name

# 3. After resolving, mark as resolved
git add resolved-file.kt

# 4. Continue merge/rebase
git merge --continue
# or
git rebase --continue

# 5. Abort if needed
git merge --abort
# or
git rebase --abort
```

### Accidentally Committed to Wrong Branch

```bash
# 1. On wrong branch, undo commit (keep changes)
git reset --soft HEAD~1

# 2. Stash changes
git stash

# 3. Switch to correct branch
git checkout correct-branch

# 4. Apply stashed changes
git stash pop

# 5. Commit on correct branch
git add .
git commit -m "feat: correct commit message"
```

### Accidentally Pushed to Wrong Branch

```bash
# 1. Revert the commit on wrong branch
git checkout wrong-branch
git revert <commit-hash>
git push origin wrong-branch

# 2. Cherry-pick to correct branch
git checkout correct-branch
git cherry-pick <commit-hash>
git push origin correct-branch
```

### Force Push Safety

```bash
# Safer than git push --force
git push --force-with-lease

# This fails if remote has commits you don't have locally
# Prevents accidentally overwriting others' work
```

### Recovering Lost Commits

```bash
# Show reflog (history of HEAD)
git reflog

# Checkout lost commit
git checkout <commit-hash>

# Create branch from lost commit
git checkout -b recovered-branch <commit-hash>
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

# Test results
*.log
test-results/
```

## Best Practices Summary

1. **Commit often** - Small, atomic commits are better than large ones
2. **Write good commit messages** - Follow conventional commits format
3. **Keep branches up to date** - Rebase regularly to avoid conflicts
4. **Review your own code** - Before requesting review
5. **Use descriptive branch names** - Include issue number and description
6. **Don't commit broken code** - Unless it's a WIP on feature branch
7. **Clean up before PR** - Use interactive rebase to organize commits
8. **Test before pushing** - Run tests locally first
9. **Use force-with-lease** - Safer than force push
10. **Delete merged branches** - Keep repository clean

## Further Reading

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/)
- [GitHub Flow](https://guides.github.com/introduction/flow/)
- [Pro Git Book](https://git-scm.com/book/en/v2)
- [Getting Started Guide](getting-started.md) - Project setup and development workflow
- [Contributing Guide](../../CONTRIBUTING.md) - Contribution guidelines
