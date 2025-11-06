# CI/CD Integration Guide

This guide explains how to integrate the Code Quality Analysis System into your CI/CD pipeline for automated code quality checks.

## Table of Contents

1. [Overview](#overview)
2. [GitHub Actions Integration](#github-actions-integration)
3. [Failure Conditions](#failure-conditions)
4. [Workflow Examples](#workflow-examples)
5. [Pull Request Integration](#pull-request-integration)
6. [Branch Protection](#branch-protection)
7. [Reporting and Notifications](#reporting-and-notifications)
8. [Performance Optimization](#performance-optimization)
9. [Troubleshooting](#troubleshooting)

---

## Overview

### Benefits of CI/CD Integration

- **Automated Quality Checks**: Run analysis on every commit/PR
- **Early Detection**: Catch issues before they reach production
- **Consistent Standards**: Enforce quality standards across team
- **Trend Tracking**: Monitor quality metrics over time
- **Fail Fast**: Block merges with critical issues

### Integration Points

1. **Pull Requests**: Analyze changed files, comment with findings
2. **Branch Pushes**: Run complete analysis on develop/main
3. **Nightly Builds**: Comprehensive analysis with trend reports
4. **Release Branches**: Quality gate before production

---

## GitHub Actions Integration

### Basic Workflow

Create `.github/workflows/code-quality.yml`:

```yaml
name: Code Quality Analysis

on:
  pull_request:
    branches: [ develop, main ]
  push:
    branches: [ develop, main ]

jobs:
  analyze:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
        with:
          fetch-depth: 0  # Full history for baseline comparison
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'oracle'
      
      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Run Code Quality Analysis
        run: ./gradlew analyzeCodeQuality --fail-on-critical
      
      - name: Upload Analysis Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: code-quality-report
          path: .kiro/specs/code-quality-analysis/analysis-report.md
          retention-days: 30
      
      - name: Comment PR with Summary
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const report = fs.readFileSync('.kiro/specs/code-quality-analysis/analysis-report.md', 'utf8');
            
            // Extract summary (first 500 chars)
            const summary = report.substring(0, 500) + '...';
            
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `## Code Quality Analysis\n\n${summary}\n\n[View Full Report](../actions/runs/${context.runId})`
            });
```

### Advanced Workflow with Matrix

Run analysis on multiple configurations:

```yaml
name: Code Quality Analysis (Matrix)

on:
  pull_request:
    branches: [ develop, main ]

jobs:
  analyze:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest]
        severity: [high, medium]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'oracle'
      
      - name: Run Analysis
        run: ./gradlew analyzeCodeQuality --severity-threshold=${{ matrix.severity }}
      
      - name: Upload Report
        uses: actions/upload-artifact@v3
        with:
          name: report-${{ matrix.os }}-${{ matrix.severity }}
          path: .kiro/specs/code-quality-analysis/analysis-report.md
```

---

## Failure Conditions

### Fail on Critical Issues

Block builds when critical issues are found:

```yaml
- name: Run Analysis (Fail on Critical)
  run: ./gradlew analyzeCodeQuality --fail-on-critical
```

This exits with code 1 if any Critical priority issues are found.

### Fail on Threshold

Fail if total issues exceed threshold:

```yaml
- name: Run Analysis
  run: ./gradlew analyzeCodeQuality
  
- name: Check Issue Count
  run: |
    ISSUE_COUNT=$(grep -oP 'Total Findings: \K\d+' .kiro/specs/code-quality-analysis/analysis-report.md)
    if [ $ISSUE_COUNT -gt 50 ]; then
      echo "Too many issues: $ISSUE_COUNT (max: 50)"
      exit 1
    fi
```

### Fail on Regression

Fail if quality metrics regress:

```yaml
- name: Run Analysis with Baseline
  run: ./gradlew analyzeCodeQuality --baseline-file=baseline-develop.json
  
- name: Check for Regressions
  run: |
    if grep -q "Regressions" .kiro/specs/code-quality-analysis/analysis-report.md; then
      echo "Quality regressions detected!"
      exit 1
    fi
```

### Custom Failure Logic

```yaml
- name: Run Analysis
  id: analysis
  run: ./gradlew analyzeCodeQuality
  continue-on-error: true
  
- name: Evaluate Results
  run: |
    CRITICAL=$(grep -oP 'Critical.*: \K\d+' .kiro/specs/code-quality-analysis/analysis-report.md)
    HIGH=$(grep -oP 'High.*: \K\d+' .kiro/specs/code-quality-analysis/analysis-report.md)
    
    if [ $CRITICAL -gt 0 ]; then
      echo "❌ Critical issues found: $CRITICAL"
      exit 1
    elif [ $HIGH -gt 5 ]; then
      echo "⚠️ Too many high priority issues: $HIGH"
      exit 1
    else
      echo "✅ Quality check passed"
    fi
```

---

## Workflow Examples

### Pull Request Workflow

Analyze only changed files in PRs:

```yaml
name: PR Code Quality

on:
  pull_request:
    branches: [ develop ]

jobs:
  analyze-pr:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'oracle'
      
      - name: Get changed files
        id: changed-files
        uses: tj-actions/changed-files@v35
        with:
          files: |
            **/*.kt
            **/*.kts
      
      - name: Analyze changed files
        if: steps.changed-files.outputs.any_changed == 'true'
        run: |
          CHANGED_FILES="${{ steps.changed-files.outputs.all_changed_files }}"
          ./gradlew analyzeCodeQuality --path="$CHANGED_FILES" --fail-on-critical
      
      - name: Comment PR
        if: always() && github.event_name == 'pull_request'
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const report = fs.readFileSync('.kiro/specs/code-quality-analysis/analysis-report.md', 'utf8');
            
            // Extract key metrics
            const criticalMatch = report.match(/Critical.*: (\d+)/);
            const highMatch = report.match(/High.*: (\d+)/);
            const critical = criticalMatch ? criticalMatch[1] : '0';
            const high = highMatch ? highMatch[1] : '0';
            
            const body = `## 📊 Code Quality Analysis
            
            **Changed Files**: ${{ steps.changed-files.outputs.all_changed_files_count }}
            
            | Priority | Count |
            |----------|-------|
            | 🔴 Critical | ${critical} |
            | 🟠 High | ${high} |
            
            ${critical > 0 ? '❌ **Critical issues must be fixed before merging**' : '✅ No critical issues found'}
            
            [View Full Report](../actions/runs/${context.runId})`;
            
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: body
            });
```

### Nightly Analysis Workflow

Comprehensive analysis with trend tracking:

```yaml
name: Nightly Code Quality

on:
  schedule:
    - cron: '0 2 * * *'  # 2 AM daily
  workflow_dispatch:  # Manual trigger

jobs:
  nightly-analysis:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'oracle'
      
      - name: Run Complete Analysis
        run: ./gradlew analyzeCodeQuality --verbose
      
      - name: Generate Trend Report
        run: ./gradlew analyzeCodeQuality --trend-report
      
      - name: Upload Reports
        uses: actions/upload-artifact@v3
        with:
          name: nightly-reports-${{ github.run_number }}
          path: |
            .kiro/specs/code-quality-analysis/analysis-report.md
            .kiro/specs/code-quality-analysis/trend-report.md
          retention-days: 90
      
      - name: Send Slack Notification
        if: failure()
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "❌ Nightly code quality analysis failed",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "Nightly code quality analysis failed. Check the report for details."
                  }
                },
                {
                  "type": "actions",
                  "elements": [
                    {
                      "type": "button",
                      "text": {
                        "type": "plain_text",
                        "text": "View Report"
                      },
                      "url": "${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}"
                    }
                  ]
                }
              ]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

### Release Quality Gate

Ensure quality before release:

```yaml
name: Release Quality Gate

on:
  push:
    branches:
      - 'release/**'

jobs:
  quality-gate:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'oracle'
      
      - name: Run Comprehensive Analysis
        run: ./gradlew analyzeCodeQuality --severity-threshold=high --fail-on-critical
      
      - name: Check Quality Metrics
        run: |
          # Extract metrics
          CRITICAL=$(grep -oP 'Critical.*: \K\d+' .kiro/specs/code-quality-analysis/analysis-report.md)
          HIGH=$(grep -oP 'High.*: \K\d+' .kiro/specs/code-quality-analysis/analysis-report.md)
          TEST_COV=$(grep -oP 'Test Coverage: \K\d+' .kiro/specs/code-quality-analysis/analysis-report.md)
          
          # Enforce release criteria
          if [ $CRITICAL -gt 0 ]; then
            echo "❌ Release blocked: $CRITICAL critical issues"
            exit 1
          fi
          
          if [ $HIGH -gt 0 ]; then
            echo "❌ Release blocked: $HIGH high priority issues"
            exit 1
          fi
          
          if [ $TEST_COV -lt 70 ]; then
            echo "❌ Release blocked: Test coverage $TEST_COV% (minimum: 70%)"
            exit 1
          fi
          
          echo "✅ Release quality gate passed"
      
      - name: Create Release Report
        run: |
          echo "# Release Quality Report" > release-quality.md
          echo "" >> release-quality.md
          echo "**Branch**: ${{ github.ref_name }}" >> release-quality.md
          echo "**Date**: $(date)" >> release-quality.md
          echo "" >> release-quality.md
          cat .kiro/specs/code-quality-analysis/analysis-report.md >> release-quality.md
      
      - name: Upload Release Report
        uses: actions/upload-artifact@v3
        with:
          name: release-quality-report
          path: release-quality.md
```

---

## Pull Request Integration

### PR Comment with Findings

```yaml
- name: Comment PR with Detailed Findings
  if: github.event_name == 'pull_request'
  uses: actions/github-script@v6
  with:
    script: |
      const fs = require('fs');
      const report = fs.readFileSync('.kiro/specs/code-quality-analysis/analysis-report.md', 'utf8');
      
      // Parse report sections
      const summaryMatch = report.match(/## Executive Summary([\s\S]*?)##/);
      const summary = summaryMatch ? summaryMatch[1].trim() : 'No summary available';
      
      // Extract critical findings
      const criticalSection = report.match(/### Critical Priority[\s\S]*?(?=###|$)/);
      const criticalFindings = criticalSection ? criticalSection[0] : 'No critical issues';
      
      const body = `## 📊 Code Quality Analysis Report
      
      ### Summary
      ${summary}
      
      ### Critical Issues
      ${criticalFindings}
      
      <details>
      <summary>View Full Report</summary>
      
      ${report}
      
      </details>
      
      ---
      *Analysis completed in ${{ steps.analysis.outputs.duration }}*`;
      
      github.rest.issues.createComment({
        issue_number: context.issue.number,
        owner: context.repo.owner,
        repo: context.repo.repo,
        body: body
      });
```

### PR Status Check

```yaml
- name: Set PR Status
  if: github.event_name == 'pull_request'
  uses: actions/github-script@v6
  with:
    script: |
      const fs = require('fs');
      const report = fs.readFileSync('.kiro/specs/code-quality-analysis/analysis-report.md', 'utf8');
      
      const criticalMatch = report.match(/Critical.*: (\d+)/);
      const critical = criticalMatch ? parseInt(criticalMatch[1]) : 0;
      
      const state = critical > 0 ? 'failure' : 'success';
      const description = critical > 0 
        ? `${critical} critical issues found` 
        : 'No critical issues';
      
      github.rest.repos.createCommitStatus({
        owner: context.repo.owner,
        repo: context.repo.repo,
        sha: context.sha,
        state: state,
        description: description,
        context: 'Code Quality Analysis'
      });
```

---

## Branch Protection

### Require Quality Checks

Configure branch protection in GitHub:

1. Go to **Settings** → **Branches**
2. Add rule for `develop` and `main`
3. Enable **Require status checks to pass**
4. Select **Code Quality Analysis**
5. Enable **Require branches to be up to date**

### Quality Gate Rules

```yaml
# .github/branch-protection.yml
branches:
  develop:
    protection:
      required_status_checks:
        strict: true
        contexts:
          - "Code Quality Analysis"
      required_pull_request_reviews:
        required_approving_review_count: 1
      enforce_admins: false
  
  main:
    protection:
      required_status_checks:
        strict: true
        contexts:
          - "Code Quality Analysis"
          - "Release Quality Gate"
      required_pull_request_reviews:
        required_approving_review_count: 2
      enforce_admins: true
```

---

## Reporting and Notifications

### Slack Notifications

```yaml
- name: Send Slack Notification
  if: always()
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "${{ job.status == 'success' && '✅' || '❌' }} Code Quality Analysis ${{ job.status }}",
        "blocks": [
          {
            "type": "section",
            "text": {
              "type": "mrkdwn",
              "text": "*Code Quality Analysis*\n*Status*: ${{ job.status }}\n*Branch*: ${{ github.ref_name }}\n*Commit*: ${{ github.sha }}"
            }
          },
          {
            "type": "actions",
            "elements": [
              {
                "type": "button",
                "text": {
                  "type": "plain_text",
                  "text": "View Report"
                },
                "url": "${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}"
              }
            ]
          }
        ]
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

### Email Notifications

```yaml
- name: Send Email Report
  if: failure()
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: smtp.gmail.com
    server_port: 465
    username: ${{ secrets.EMAIL_USERNAME }}
    password: ${{ secrets.EMAIL_PASSWORD }}
    subject: "❌ Code Quality Analysis Failed"
    to: team@shoppit.com
    from: ci@shoppit.com
    body: |
      Code quality analysis failed for ${{ github.ref_name }}.
      
      View report: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}
    attachments: .kiro/specs/code-quality-analysis/analysis-report.md
```

---

## Performance Optimization

### Cache Gradle Dependencies

```yaml
- name: Cache Gradle packages
  uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    restore-keys: |
      ${{ runner.os }}-gradle-
```

### Parallel Analysis

```yaml
- name: Run Analysis in Parallel
  run: ./gradlew analyzeCodeQuality --parallel --max-workers=4
```

### Incremental Analysis

```yaml
- name: Analyze Changed Files Only
  run: |
    CHANGED_FILES=$(git diff --name-only ${{ github.event.before }} ${{ github.sha }} | grep '\.kt$' | tr '\n' ',')
    if [ -n "$CHANGED_FILES" ]; then
      ./gradlew analyzeCodeQuality --path="$CHANGED_FILES"
    fi
```

---

## Troubleshooting

### Common Issues

**Issue**: Workflow fails with "Permission denied"

**Solution**:
```yaml
- name: Grant execute permission
  run: chmod +x gradlew
```

**Issue**: Out of memory during analysis

**Solution**:
```yaml
- name: Run Analysis with More Memory
  run: ./gradlew analyzeCodeQuality -Xmx4g
  env:
    GRADLE_OPTS: "-Xmx4g"
```

**Issue**: Analysis takes too long

**Solution**:
```yaml
# Use incremental analysis
- name: Analyze Changed Files
  run: ./gradlew analyzeCodeQuality --path="$CHANGED_FILES"

# Or increase timeout
- name: Run Analysis
  run: ./gradlew analyzeCodeQuality
  timeout-minutes: 30
```

**Issue**: Report not uploaded

**Solution**:
```yaml
- name: Upload Report
  if: always()  # Upload even if analysis fails
  uses: actions/upload-artifact@v3
  with:
    name: code-quality-report
    path: .kiro/specs/code-quality-analysis/analysis-report.md
    if-no-files-found: warn
```

### Debug Mode

```yaml
- name: Run Analysis with Debug Logging
  run: ./gradlew analyzeCodeQuality --verbose --stacktrace
```

---

## Best Practices

### 1. Run on Every PR

Catch issues early before they reach main branches.

### 2. Fail Fast on Critical Issues

Block merges when critical issues are found.

### 3. Track Trends Over Time

Run nightly analysis to monitor quality metrics.

### 4. Provide Clear Feedback

Comment on PRs with actionable findings.

### 5. Optimize for Speed

Use incremental analysis and caching.

### 6. Set Realistic Thresholds

Don't block all PRs - focus on critical issues.

### 7. Integrate with Code Review

Use analysis results to guide code reviews.

### 8. Monitor CI/CD Performance

Track analysis duration and optimize as needed.

---

## Next Steps

1. **Set up basic workflow**: Start with PR analysis
2. **Configure failure conditions**: Define quality gates
3. **Add notifications**: Keep team informed
4. **Optimize performance**: Enable caching and parallel execution
5. **Monitor and adjust**: Refine thresholds based on team feedback

For more information:
- [Usage Guide](USAGE_GUIDE.md)
- [Analyzer Reference](ANALYZER_REFERENCE.md)
- [Example Reports](examples/)
