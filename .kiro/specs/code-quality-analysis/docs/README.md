# Code Quality Analysis System - Documentation

Welcome to the Code Quality Analysis System documentation for the Shoppit Android application.

## Quick Start

1. **Run your first analysis**:
   ```powershell
   .\gradlew.bat analyzeCodeQuality
   ```

2. **View the report**:
   - Open `.kiro/specs/code-quality-analysis/analysis-report.md`

3. **Address critical issues**:
   - Focus on Critical and High priority findings first

## Documentation Structure

### 📘 [Usage Guide](USAGE_GUIDE.md)

Complete guide to running and configuring the analysis system:
- Getting started
- Command-line options
- Configuration
- Understanding reports
- Working with baselines
- Incremental analysis
- Troubleshooting

**Start here if**: You're new to the system or need to run analysis.

### 📗 [Analyzer Reference](ANALYZER_REFERENCE.md)

Detailed documentation for each analyzer:
- What each analyzer checks
- Examples of violations
- How to fix issues
- Code examples (before/after)

**Start here if**: You want to understand specific findings or learn about best practices.

### 📙 [Example Reports](examples/)

Sample analysis reports demonstrating output:
- [Sample Report](examples/sample-report.md) - Comprehensive example
- [Examples README](examples/README.md) - How to use examples

**Start here if**: You want to see what reports look like or understand report structure.

### 📕 [CI/CD Integration Guide](CI_CD_INTEGRATION.md)

Guide to integrating analysis into CI/CD pipelines:
- GitHub Actions workflows
- Failure conditions
- Pull request integration
- Branch protection
- Notifications
- Performance optimization

**Start here if**: You want to automate analysis in your CI/CD pipeline.

## Quick Reference

### Common Commands

```powershell
# Complete analysis
.\gradlew.bat analyzeCodeQuality

# Analyze specific directory
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui

# High severity only
.\gradlew.bat analyzeCodeQuality --severity-threshold=high

# Specific analyzers
.\gradlew.bat analyzeCodeQuality --analyzers=architecture,security

# Generate baseline
.\gradlew.bat analyzeCodeQuality --baseline

# CI/CD mode (fail on critical)
.\gradlew.bat analyzeCodeQuality --fail-on-critical
```

### Priority Levels

| Priority | Action | Timeline |
|----------|--------|----------|
| **CRITICAL** | Fix immediately | Before commit |
| **HIGH** | Fix before release | Current sprint |
| **MEDIUM** | Schedule refactoring | Next sprint |
| **LOW** | Fix when convenient | Backlog |

### Analyzers

| Analyzer | Focus | Priority |
|----------|-------|----------|
| **Code Smell** | Maintainability | MEDIUM |
| **Architecture** | Layer separation | HIGH |
| **Compose** | UI best practices | MEDIUM |
| **State Management** | State patterns | HIGH |
| **Error Handling** | Exception handling | HIGH |
| **Dependency Injection** | Hilt usage | MEDIUM |
| **Database** | Room patterns | HIGH |
| **Performance** | Optimization | MEDIUM |
| **Naming** | Conventions | LOW |
| **Test Coverage** | Test presence | MEDIUM |
| **Documentation** | KDoc comments | LOW |
| **Security** | Security issues | CRITICAL |

## Workflows

### Development Workflow

1. **Before starting work**:
   ```powershell
   git checkout develop
   git pull origin develop
   git checkout -b feature/123-my-feature
   ```

2. **During development**:
   ```powershell
   # Analyze your changes
   .\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui/meal
   
   # Fix critical and high issues
   # Commit changes
   ```

3. **Before creating PR**:
   ```powershell
   # Run complete analysis
   .\gradlew.bat analyzeCodeQuality --fail-on-critical
   
   # Address any critical issues
   # Push to remote
   ```

### Code Review Workflow

1. **Reviewer runs analysis**:
   ```powershell
   git checkout feature/123-my-feature
   .\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui/meal
   ```

2. **Review findings**:
   - Check for new critical/high issues
   - Verify fixes for existing issues
   - Provide feedback

3. **Approve or request changes**:
   - Approve if no critical issues
   - Request changes if issues found

### Release Workflow

1. **Create release branch**:
   ```powershell
   git checkout -b release/1.2.0
   ```

2. **Run comprehensive analysis**:
   ```powershell
   .\gradlew.bat analyzeCodeQuality --severity-threshold=high --fail-on-critical
   ```

3. **Quality gate**:
   - ✅ No critical issues
   - ✅ No high priority issues
   - ✅ Test coverage > 70%
   - ✅ Documentation coverage > 50%

4. **Generate release report**:
   - Save analysis report
   - Document accepted risks
   - Update CHANGELOG

## Integration Points

### With Existing Tools

**Detekt**:
- Automatically integrated
- Findings included in report
- Configuration: `app/detekt-config.yml`

**Snyk**:
- Run after analysis
- Security scan recommendations
- Update `SECURITY_ISSUES.md`

**Git**:
- Pre-commit hooks
- Branch protection
- PR checks

### With Project Documentation

- [Project Structure](../../../docs/structure.md)
- [Compose Patterns](../../../docs/compose-patterns.md)
- [Data Layer Patterns](../../../docs/data-layer-patterns.md)
- [Error Handling](../../../docs/error-handling.md)
- [Testing Strategy](../../../docs/testing-strategy.md)
- [Security Best Practices](../../../docs/security.md)

## Support

### Getting Help

1. **Check documentation**:
   - [Usage Guide](USAGE_GUIDE.md) for how-to questions
   - [Analyzer Reference](ANALYZER_REFERENCE.md) for understanding findings
   - [CI/CD Guide](CI_CD_INTEGRATION.md) for automation

2. **Review examples**:
   - [Example Reports](examples/) for report interpretation

3. **Troubleshooting**:
   - See [Usage Guide - Troubleshooting](USAGE_GUIDE.md#troubleshooting)
   - Check [CI/CD Guide - Troubleshooting](CI_CD_INTEGRATION.md#troubleshooting)

### Reporting Issues

When reporting issues, include:
- Command used
- Error message
- Gradle version: `.\gradlew.bat --version`
- Sample code (if applicable)
- Expected vs actual behavior

## Contributing

### Adding New Analyzers

1. Implement `CodeAnalyzer` interface
2. Add tests
3. Update documentation:
   - Add to [Analyzer Reference](ANALYZER_REFERENCE.md)
   - Update this README
   - Add examples

### Improving Documentation

1. Identify gaps or unclear sections
2. Create/update documentation
3. Add examples
4. Submit PR

## Roadmap

### Current Version (1.0)

- ✅ 12 analyzers covering all major categories
- ✅ Comprehensive reporting
- ✅ Baseline tracking
- ✅ CI/CD integration
- ✅ Complete documentation

### Future Enhancements

- 🔄 Auto-fix capabilities
- 🔄 IDE integration
- 🔄 Custom rule configuration
- 🔄 Machine learning-based suggestions
- 🔄 Real-time analysis during development

## License

This analysis system is part of the Shoppit Android application and follows the same license.

## Acknowledgments

Built with:
- Kotlin Compiler (PSI)
- Detekt
- Gradle
- GitHub Actions

Inspired by:
- Clean Architecture principles
- Android best practices
- Jetpack Compose guidelines
- Industry-standard static analysis tools

---

**Last Updated**: 2024-11-06  
**Version**: 1.0  
**Maintainer**: Shoppit Development Team
