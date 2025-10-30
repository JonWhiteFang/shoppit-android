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
feature/123-meal-planner-ui
feature/456-shopping-list-aggregation
```

### Bugfix Branches
```
bugfix/<issue-number>-<short-description>
bugfix/789-fix-ingredient-duplication
bugfix/101-crash-on-empty-meal
```

### Hotfix Branches
```
hotfix/<version>-<short-description>
hotfix/1.2.1-critical-data-loss
```

### Release Branches
```
release/<version>
release/1.3.0
```

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
- Combines all feature commits into single commit on develop
- Keeps develop history clean
- Use for feature branches

### Rebase and Merge
- Maintains individual commits
- Linear history
- Use for small, well-organized branches

### Merge Commit
- Preserves full branch history
- Creates merge commit
- Use for release branches

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
```bash
# View conflicted files
git status

# Edit files to resolve conflicts
# Look for <<<<<<< HEAD markers

# Mark as resolved
git add resolved-file.kt

# Continue merge/rebase
git merge --continue
# or
git rebase --continue
```

### Accidentally Committed to Wrong Branch
```bash
# On wrong branch
git reset --soft HEAD~1

# Switch to correct branch
git checkout correct-branch

# Commit changes
git add .
git commit -m "feat: correct commit message"
```

### Force Push Safety
```bash
# Safer than git push --force
git push --force-with-lease

# This fails if remote has commits you don't have locally
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

### Common Scopes
- `meal`, `planner`, `shopping` - Features
- `data`, `domain`, `ui` - Layers
