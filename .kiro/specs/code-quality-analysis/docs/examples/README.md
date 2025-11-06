# Example Reports

This directory contains example analysis reports demonstrating the output of the Code Quality Analysis System.

## Available Examples

### 1. Sample Report (`sample-report.md`)

A comprehensive example report showing:
- Executive summary with key metrics
- Findings organized by priority (Critical, High, Medium, Low)
- Detailed findings with code examples
- Before/after code comparisons
- Improvement recommendations
- Baseline comparison
- Next steps

**Use this example to**:
- Understand report structure
- See how findings are presented
- Learn how to interpret metrics
- Plan remediation efforts

### 2. Minimal Report (`minimal-report.md`)

A simplified report for a small codebase with few issues:
- Basic metrics
- Few findings
- Simple recommendations

**Use this example to**:
- See what a "clean" codebase report looks like
- Understand baseline establishment
- Set quality goals

### 3. Critical Issues Report (`critical-issues-report.md`)

A focused report showing only critical and high priority issues:
- Security vulnerabilities
- Architecture violations
- Critical bugs

**Use this example to**:
- Prioritize urgent fixes
- Prepare for production releases
- Focus on high-impact issues

## How to Use These Examples

### For Developers

1. **First Time Running Analysis**:
   - Review `sample-report.md` to understand what to expect
   - Compare your first report with the sample
   - Focus on Critical and High priority issues first

2. **Regular Analysis**:
   - Use your report to track progress
   - Compare with baseline to see improvements
   - Address issues by priority

3. **Code Reviews**:
   - Run analysis on feature branches
   - Compare with develop branch baseline
   - Address new issues before merging

### For Team Leads

1. **Setting Quality Goals**:
   - Use `minimal-report.md` as a target state
   - Track metrics over time
   - Set improvement targets (e.g., reduce complexity by 10%)

2. **Sprint Planning**:
   - Review `critical-issues-report.md` for urgent items
   - Allocate time for technical debt
   - Track progress sprint-over-sprint

3. **Release Planning**:
   - Ensure no Critical issues before release
   - Address High priority issues
   - Document accepted Medium/Low issues

### For CI/CD Integration

1. **Pull Request Checks**:
   - Run analysis on changed files
   - Fail PR if Critical issues introduced
   - Comment with findings summary

2. **Nightly Builds**:
   - Run complete analysis
   - Generate trend reports
   - Alert on regressions

3. **Release Branches**:
   - Run comprehensive analysis
   - Generate release quality report
   - Block release if Critical issues exist

## Interpreting Reports

### Priority Levels

| Priority | Action Required | Timeline |
|----------|----------------|----------|
| **CRITICAL** | Fix immediately | Before commit |
| **HIGH** | Fix before release | Current sprint |
| **MEDIUM** | Schedule for refactoring | Next sprint |
| **LOW** | Fix when convenient | Backlog |

### Metrics

**Cyclomatic Complexity**:
- < 10: Good
- 10-15: Acceptable
- 15-20: Consider refactoring
- > 20: Refactor required

**Function Length**:
- < 30 lines: Good
- 30-50 lines: Acceptable
- > 50 lines: Consider splitting

**Class Length**:
- < 200 lines: Good
- 200-300 lines: Acceptable
- > 300 lines: Consider splitting

**Test Coverage**:
- > 80%: Excellent
- 60-80%: Good
- 40-60%: Needs improvement
- < 40%: Poor

### Baseline Comparison

**Improvements** (↓):
- Positive change
- Issues resolved
- Metrics improved

**Regressions** (↑):
- Negative change
- New issues introduced
- Metrics degraded

**No Change** (→):
- Stable metrics
- No new issues
- No resolved issues

## Customizing Reports

### Filtering by Severity

```powershell
# Only Critical and High
.\gradlew.bat analyzeCodeQuality --severity-threshold=high
```

### Filtering by Category

```powershell
# Only Architecture and Security
.\gradlew.bat analyzeCodeQuality --analyzers=architecture,security
```

### Custom Output

```powershell
# Custom output path
.\gradlew.bat analyzeCodeQuality --output=reports/quality-$(Get-Date -Format "yyyy-MM-dd").md
```

## Report Sections Explained

### Executive Summary

Quick overview of:
- Total findings by priority
- Key metrics and trends
- Top issues requiring attention
- Improvements since baseline

**Use for**: Quick status check, management reporting

### Findings by Priority

Detailed list of all issues:
- Organized by priority level
- Grouped by category
- Includes code examples
- Provides fix recommendations

**Use for**: Development work, code reviews

### Improvement Recommendations

Actionable next steps:
- Quick wins (< 1 hour)
- Short term (1-4 hours)
- Medium term (1-2 days)
- Long term (> 2 days)

**Use for**: Sprint planning, backlog grooming

### Baseline Comparison

Historical perspective:
- Improvements over time
- Regressions to address
- Metric trends
- Progress tracking

**Use for**: Retrospectives, progress reports

## Next Steps

1. **Run your first analysis**:
   ```powershell
   .\gradlew.bat analyzeCodeQuality
   ```

2. **Review your report**:
   - Compare with `sample-report.md`
   - Identify Critical and High issues
   - Plan remediation

3. **Establish baseline**:
   ```powershell
   .\gradlew.bat analyzeCodeQuality --baseline
   ```

4. **Track progress**:
   - Run analysis regularly
   - Compare with baseline
   - Celebrate improvements

## Additional Resources

- [Usage Guide](../USAGE_GUIDE.md) - How to run analysis
- [Analyzer Reference](../ANALYZER_REFERENCE.md) - What each analyzer checks
- [CI/CD Integration](../CI_CD_INTEGRATION.md) - Automated analysis
- [Project Documentation](../../../../docs/) - Shoppit coding standards
