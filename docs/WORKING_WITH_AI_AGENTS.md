# Working with AI Agents

## Overview

This document provides guidelines for working with AI agents (like Kiro) on the Shoppit Android project. It covers best practices, workflows, common patterns, and how to get the most out of AI-assisted development.

## Table of Contents

- [Getting Started](#getting-started)
- [Spec-Driven Development](#spec-driven-development)
- [Agent Capabilities](#agent-capabilities)
- [Communication Best Practices](#communication-best-practices)
- [Common Workflows](#common-workflows)
- [Task Execution](#task-execution)
- [Code Review with Agents](#code-review-with-agents)
- [Troubleshooting](#troubleshooting)
- [Security Considerations](#security-considerations)
- [Tips and Tricks](#tips-and-tricks)

---

## Getting Started

### First Interaction

When starting a new session with an AI agent:

1. **Provide Context**: Share what you're working on
2. **Be Specific**: Clear requests get better results
3. **Reference Files**: Use `#File` or `#Folder` to include context
4. **Set Expectations**: Mention if you want exploration vs. implementation

**Example:**
```
I want to add a meal categories feature to the app. 
Can you help me create a spec for this? 
#Folder app/src/main/java/com/shoppit/app
```

### Understanding Agent Capabilities

The agent can:
- ✅ Read and analyze code across the entire project
- ✅ Create, modify, and delete files
- ✅ Run Gradle commands and tests
- ✅ Execute security scans with Snyk
- ✅ Search documentation and web resources
- ✅ Create comprehensive specs and implementation plans
- ✅ Generate code following project patterns
- ✅ Debug issues and suggest fixes

The agent cannot:
- ❌ Run the app on a device/emulator
- ❌ Interact with the Android Studio UI
- ❌ Access external services without credentials
- ❌ Make subjective design decisions without guidance

---

## Spec-Driven Development

### What is a Spec?

A spec is a structured approach to building features:
1. **Requirements**: User stories and acceptance criteria
2. **Design**: Architecture, components, data models
3. **Tasks**: Step-by-step implementation plan

### Creating a New Spec

**Request:**
```
I want to create a spec for adding meal categories to the app.
Users should be able to tag meals with categories like "Breakfast", "Dinner", etc.
```

**The agent will:**
1. Create `.kiro/specs/meal-categories/requirements.md`
2. Ask for your review and approval
3. Create `.kiro/specs/meal-categories/design.md`
4. Ask for your review and approval
5. Create `.kiro/specs/meal-categories/tasks.md`
6. Ask for your review and approval

### Reviewing Specs

At each stage, the agent will ask:
- "Do the requirements look good? If so, we can move on to the design."
- "Does the design look good? If so, we can move on to the implementation plan."
- "Does the task list look good?"

**Your options:**
- ✅ **Approve**: "Yes, looks good" or "Approved"
- 🔄 **Request Changes**: "Can you add X?" or "Change Y to Z"
- ⏮️ **Go Back**: "Let's revise the requirements first"

### Executing Tasks from a Spec

Once the spec is complete:

**Request:**
```
Let's start implementing the meal categories feature.
Can you execute task 1 from the spec?
```

**The agent will:**
1. Read the requirements, design, and tasks
2. Implement only the requested task
3. Run security scans (mandatory)
4. Update SECURITY_ISSUES.md
5. Stop and wait for your review

**Important:** The agent executes ONE task at a time and waits for your approval before continuing.

---

## Agent Capabilities

### Code Generation

**What the agent does well:**
- Following established patterns (Clean Architecture, MVVM)
- Implementing data models, repositories, use cases
- Creating ViewModels with proper state management
- Building Compose UI components
- Writing unit tests following project conventions
- Applying dependency injection with Hilt

**Example request:**
```
Create a MealCategoryRepository following the project's repository pattern.
It should support CRUD operations and return Flow for reactive queries.
#File app/src/main/java/com/shoppit/app/data/repository/MealRepositoryImpl.kt
```

### Code Analysis

**What the agent can analyze:**
- Architecture violations
- Code smells and anti-patterns
- Performance issues
- Security vulnerabilities
- Test coverage gaps
- Accessibility issues

**Example request:**
```
Review the MealViewModel for any state management issues or performance problems.
#File app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt
```

### Refactoring

**What the agent can refactor:**
- Extract reusable components
- Improve code organization
- Apply design patterns
- Optimize performance
- Enhance error handling

**Example request:**
```
The MealListScreen is getting too large. 
Can you extract the meal card into a separate reusable component?
#File app/src/main/java/com/shoppit/app/ui/meal/MealListScreen.kt
```

### Testing

**What the agent can test:**
- Generate unit tests for ViewModels, use cases, repositories
- Create instrumented tests for DAOs and UI
- Write test data builders
- Mock dependencies with MockK
- Test error scenarios

**Example request:**
```
Write comprehensive unit tests for AddMealUseCase covering success and validation error cases.
#File app/src/main/java/com/shoppit/app/domain/usecase/AddMealUseCase.kt
```

### Documentation

**What the agent can document:**
- Code comments for complex logic
- KDoc for public APIs
- Architecture decision records
- Feature documentation
- API documentation

**Example request:**
```
Add KDoc comments to the MealRepository interface explaining each method's purpose and return values.
#File app/src/main/java/com/shoppit/app/domain/repository/MealRepository.kt
```

---

## Communication Best Practices

### Be Specific

❌ **Vague:** "Fix the meal screen"
✅ **Specific:** "The MealListScreen crashes when the list is empty. Can you add an empty state?"

❌ **Vague:** "Add tests"
✅ **Specific:** "Write unit tests for MealViewModel covering the loading, success, and error states"

### Provide Context

**Use context markers:**
- `#File` - Include a specific file
- `#Folder` - Include a directory
- `#Codebase` - Search the entire codebase
- `#Problems` - Include current IDE problems
- `#Terminal` - Include terminal output

**Example:**
```
The build is failing with a dependency conflict.
#Terminal
Can you help resolve this?
```

### Break Down Complex Requests

❌ **Too broad:** "Implement the entire meal planner feature"
✅ **Broken down:** 
```
Let's implement the meal planner feature step by step:
1. First, create the data models and database schema
2. Then, implement the repository and use cases
3. Finally, build the UI
```

### Iterate and Refine

Don't expect perfection on the first try. Iterate:

**First request:**
```
Create a MealCard component for displaying meals in a list.
```

**Follow-up:**
```
Can you add a swipe-to-delete gesture to the MealCard?
```

**Further refinement:**
```
The delete animation is too abrupt. Can you add a confirmation dialog?
```

### Ask for Explanations

The agent can explain its decisions:

```
Why did you use StateFlow instead of LiveData in the ViewModel?
```

```
Can you explain the error handling strategy in the repository?
```

---

## Common Workflows

### Workflow 1: Adding a New Feature

**Step 1: Create Spec**
```
I want to add a feature for meal categories. 
Users should be able to assign categories to meals and filter by category.
Can you help me create a spec?
```

**Step 2: Review Requirements**
- Agent creates requirements.md
- You review and approve or request changes

**Step 3: Review Design**
- Agent creates design.md
- You review and approve or request changes

**Step 4: Review Tasks**
- Agent creates tasks.md
- You review and approve or request changes

**Step 5: Execute Tasks**
```
Let's start with task 1: Create the category data model and database schema.
```

**Step 6: Iterate**
- Agent implements task 1
- Runs security scans
- Updates documentation
- You review and approve
- Move to task 2

### Workflow 2: Fixing a Bug

**Step 1: Describe the Bug**
```
The app crashes when adding a meal with no ingredients.
#File app/src/main/java/com/shoppit/app/ui/meal/AddMealScreen.kt
#Problems
```

**Step 2: Agent Analyzes**
- Agent reads the file
- Identifies the issue
- Proposes a fix

**Step 3: Review and Apply**
- You review the proposed fix
- Agent applies the fix
- Agent runs tests to verify

**Step 4: Verify**
```
Can you write a test to ensure this bug doesn't happen again?
```

### Workflow 3: Refactoring

**Step 1: Identify Refactoring Need**
```
The MealViewModel is getting too large and hard to maintain.
Can you suggest a refactoring approach?
#File app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt
```

**Step 2: Agent Proposes**
- Agent analyzes the code
- Suggests refactoring strategy
- Explains benefits and trade-offs

**Step 3: Execute Refactoring**
```
Let's proceed with extracting the validation logic into a separate validator class.
```

**Step 4: Verify**
- Agent refactors the code
- Runs tests to ensure no regressions
- Updates related files

### Workflow 4: Code Review

**Step 1: Request Review**
```
Can you review this ViewModel for any issues?
#File app/src/main/java/com/shoppit/app/ui/planner/PlannerViewModel.kt
```

**Step 2: Agent Reviews**
- Checks for architecture violations
- Identifies potential bugs
- Suggests improvements
- Highlights security concerns

**Step 3: Address Feedback**
```
Can you fix the state management issue you identified?
```

### Workflow 5: Writing Tests

**Step 1: Request Tests**
```
Write comprehensive unit tests for the GetMealsUseCase.
#File app/src/main/java/com/shoppit/app/domain/usecase/GetMealsUseCase.kt
```

**Step 2: Agent Generates Tests**
- Creates test file
- Covers success cases
- Covers error cases
- Uses project testing patterns

**Step 3: Run and Verify**
```
Can you run the tests to make sure they pass?
```

---

## Task Execution

### Understanding Task Execution

When executing tasks from a spec:

**Key principles:**
1. **One task at a time**: Agent completes one task, then stops
2. **Security scans mandatory**: Every code change triggers security scans
3. **Documentation updates**: SECURITY_ISSUES.md is always updated
4. **No automatic continuation**: Agent waits for your approval before next task

### Executing a Task

**Request:**
```
Execute task 2.1 from the meal-categories spec.
```

**Agent will:**
1. Read requirements.md, design.md, tasks.md
2. Implement only task 2.1
3. Run `pwd` to get absolute path
4. Run `snyk_code_scan` on modified files
5. Review scan results
6. Fix any Critical/High issues
7. Update SECURITY_ISSUES.md
8. Stop and report completion

### Continuing to Next Task

**After reviewing task 2.1:**
```
Looks good! Let's move to task 2.2.
```

**Or if changes needed:**
```
Can you also add validation for empty category names?
```

### Skipping Optional Tasks

Tasks marked with `*` are optional (usually tests):

```
Skip task 2.4 (unit tests) for now. Let's move to task 3.
```

### Executing Multiple Related Tasks

**Request:**
```
Execute tasks 2.1, 2.2, and 2.3 together since they're all related to the data model.
```

**Note:** Agent will still execute them sequentially and run security scans after all changes.

---

## Code Review with Agents

### Requesting a Code Review

**General review:**
```
Can you review the meal planner feature for any issues?
#Folder app/src/main/java/com/shoppit/app/ui/planner
```

**Specific focus:**
```
Review the MealViewModel for state management and performance issues.
#File app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt
```

**Architecture review:**
```
Check if the repository implementation follows Clean Architecture principles.
#File app/src/main/java/com/shoppit/app/data/repository/MealRepositoryImpl.kt
```

### What the Agent Reviews

**Code Quality:**
- Adherence to project patterns
- Code organization and structure
- Naming conventions
- Code duplication

**Architecture:**
- Layer separation (Data, Domain, UI)
- Dependency direction
- Single Responsibility Principle
- Dependency injection usage

**Performance:**
- Unnecessary recompositions
- Heavy computations without `remember`
- Inefficient database queries
- Memory leaks

**Security:**
- Input validation
- SQL injection risks
- Hardcoded secrets
- Insecure data storage

**Testing:**
- Test coverage
- Test quality
- Missing edge cases
- Flaky tests

**Accessibility:**
- Content descriptions
- Touch target sizes
- Color contrast
- Keyboard navigation

### Acting on Review Feedback

**Request fixes:**
```
Can you fix the state management issue you identified?
```

**Request explanation:**
```
Why is the current approach problematic? What are the benefits of your suggestion?
```

**Defer fixes:**
```
Let's create a task to address the performance issue later. For now, let's focus on functionality.
```

---

## Troubleshooting

### Build Failures

**Request:**
```
The build is failing. Can you help?
#Terminal
```

**Agent will:**
1. Analyze the error message
2. Identify the root cause
3. Propose a fix
4. Apply the fix if you approve

### Test Failures

**Request:**
```
The MealViewModelTest is failing. Can you investigate?
#Terminal
#File app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTest.kt
```

**Agent will:**
1. Read the test file
2. Analyze the failure message
3. Identify the issue
4. Fix the test or the implementation

### Runtime Crashes

**Request:**
```
The app crashes when I click on a meal. Here's the stack trace:
[paste stack trace]
#File app/src/main/java/com/shoppit/app/ui/meal/MealDetailScreen.kt
```

**Agent will:**
1. Analyze the stack trace
2. Identify the problematic code
3. Propose a fix
4. Add defensive programming if needed

### Performance Issues

**Request:**
```
The meal list is laggy when scrolling. Can you investigate?
#File app/src/main/java/com/shoppit/app/ui/meal/MealListScreen.kt
```

**Agent will:**
1. Analyze the composable
2. Identify performance bottlenecks
3. Suggest optimizations
4. Implement improvements

### Security Scan Issues

**Request:**
```
The security scan found a SQL injection vulnerability. Can you fix it?
#File app/src/main/java/com/shoppit/app/data/local/dao/MealDao.kt
```

**Agent will:**
1. Review the vulnerable code
2. Explain the security risk
3. Implement a secure fix
4. Re-run the security scan

---

## Security Considerations

### Mandatory Security Workflow

**Every code change MUST include:**
1. Security scan (SAST or SCA)
2. Review of scan results
3. Fix Critical/High issues
4. Update SECURITY_ISSUES.md

**The agent automatically:**
- Runs `pwd` to get absolute path
- Executes `snyk_code_scan` for code changes
- Executes `snyk_sca_scan` for dependency changes
- Reviews and addresses findings
- Updates security documentation

### What Gets Scanned

**Code changes (SAST):**
- New `.kt` or `.java` files
- Modified `.kt` or `.java` files
- Any source code changes

**Dependency changes (SCA):**
- Changes to `build.gradle.kts`
- Changes to `gradle/libs.versions.toml`
- Changes to `settings.gradle.kts`

### Handling Security Findings

**Critical/High severity:**
- MUST be fixed before task completion
- If cannot fix, MUST document as accepted risk

**Medium severity:**
- Fix if straightforward
- Document if accepting risk

**Low severity:**
- Review and document
- Can be accepted with brief justification

### Security Documentation

**SECURITY_ISSUES.md must include:**
- Scan date and results summary
- Active vulnerabilities
- Fixed issues
- Accepted risks with justification

---

## Tips and Tricks

### Tip 1: Use Context Effectively

**Include relevant files:**
```
I want to add a new field to the Meal model.
#File app/src/main/java/com/shoppit/app/domain/model/Meal.kt
#File app/src/main/java/com/shoppit/app/data/local/entity/MealEntity.kt
```

### Tip 2: Reference Examples

**Point to existing patterns:**
```
Create a ShoppingListViewModel following the same pattern as MealViewModel.
#File app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt
```

### Tip 3: Ask for Alternatives

**Explore options:**
```
What are the pros and cons of using a sealed class vs. data class for the UI state?
```

### Tip 4: Request Explanations

**Understand the code:**
```
Can you explain how the ingredient aggregation logic works?
#File app/src/main/java/com/shoppit/app/domain/usecase/GenerateShoppingListUseCase.kt
```

### Tip 5: Iterate Incrementally

**Start simple, then enhance:**
```
First, create a basic MealCard component.
[Review]
Now add a favorite button.
[Review]
Now add swipe-to-delete.
```

### Tip 6: Use Specs for Complex Features

**For anything non-trivial:**
```
This feature is complex. Let's create a spec first before implementing.
```

### Tip 7: Leverage Agent's Knowledge

**Ask about best practices:**
```
What's the best way to handle pagination in a LazyColumn with Room?
```

### Tip 8: Request Code Reviews Regularly

**Before committing:**
```
Can you review my changes before I commit?
#Folder app/src/main/java/com/shoppit/app/ui/planner
```

### Tip 9: Ask for Test Suggestions

**Improve test coverage:**
```
What test cases am I missing for the MealRepository?
#File app/src/test/java/com/shoppit/app/data/repository/MealRepositoryImplTest.kt
```

### Tip 10: Use Agent for Documentation

**Keep docs updated:**
```
Update the README with instructions for the new meal categories feature.
#File README.md
```

---

## Advanced Patterns

### Pattern 1: Exploratory Analysis

**Before implementing:**
```
I'm thinking about adding meal categories. 
Can you analyze the current codebase and suggest how this would fit into the architecture?
#Codebase
```

### Pattern 2: Comparative Analysis

**Evaluate options:**
```
Compare using Room relations vs. manual joins for the meal-ingredient relationship.
What are the trade-offs?
```

### Pattern 3: Incremental Refactoring

**Refactor safely:**
```
Let's refactor the MealViewModel in small steps:
1. First, extract validation logic
2. Then, simplify state management
3. Finally, improve error handling
```

### Pattern 4: Test-Driven Development

**Write tests first:**
```
Write failing tests for the AddMealUseCase validation logic.
[Review tests]
Now implement the validation to make the tests pass.
```

### Pattern 5: Documentation-Driven Development

**Document first:**
```
Create KDoc for the MealRepository interface describing the expected behavior.
[Review docs]
Now implement the repository following the documented contract.
```

---

## Common Mistakes to Avoid

### Mistake 1: Vague Requests

❌ "Fix the app"
✅ "The MealListScreen crashes when the list is empty. Can you add an empty state?"

### Mistake 2: Skipping Security Scans

❌ "Skip the security scan, I'll run it later"
✅ Let the agent run mandatory security scans

### Mistake 3: Not Providing Context

❌ "Add a button"
✅ "Add a delete button to the MealCard component" + include file

### Mistake 4: Expecting Mind Reading

❌ "Make it better"
✅ "Improve the error handling by adding specific error messages for each failure case"

### Mistake 5: Rushing Through Specs

❌ "Just implement it, we don't need a spec"
✅ Create a spec for complex features to ensure alignment

### Mistake 6: Not Reviewing Agent Output

❌ Blindly accepting all changes
✅ Review code, ask questions, request modifications

### Mistake 7: Ignoring Test Failures

❌ "The tests are failing but the code works"
✅ Fix failing tests or update them if behavior changed

### Mistake 8: Overloading Requests

❌ "Implement features A, B, C, D, and E"
✅ "Let's start with feature A, then move to B"

---

## Quick Reference

### Starting a New Feature
```
Create a spec for [feature name].
[Describe feature requirements]
```

### Executing Tasks
```
Execute task [number] from the [spec-name] spec.
```

### Requesting Code Review
```
Review [file/folder] for [specific concerns].
#File or #Folder
```

### Fixing Issues
```
[Describe issue]
#File [relevant file]
#Terminal [if applicable]
```

### Writing Tests
```
Write [unit/instrumented] tests for [component].
#File [file to test]
```

### Refactoring
```
Refactor [component] to [desired outcome].
#File [file to refactor]
```

### Getting Help
```
How do I [task] in this project?
#Codebase [if relevant]
```

---

## Conclusion

Working with AI agents is a collaborative process. The agent is a powerful tool that can significantly accelerate development, but it works best when you:

1. **Communicate clearly** - Be specific about what you want
2. **Provide context** - Include relevant files and information
3. **Review carefully** - Don't blindly accept all suggestions
4. **Iterate** - Refine requests based on results
5. **Follow workflows** - Use specs for complex features
6. **Maintain security** - Let mandatory scans run
7. **Ask questions** - Understand the code being generated

The agent is here to help you build better software faster. Use it effectively, and you'll find it to be an invaluable development partner.

---

## Additional Resources

- [Spec Workflow Documentation](../.kiro/specs/README.md)
- [Project Structure](structure.md)
- [Testing Strategy](testing-strategy.md)
- [Security Best Practices](security.md)
- [Git Workflow](git-workflow.md)

---

**Last Updated:** 2025-11-05
**Maintained By:** Development Team
