# CI/CD Integration Guide

This guide explains how to integrate the Code Quality Analysis System into your CI/CD pipeline.

## Table of Contents

1. [GitHub Actions](#github-actions)
2. [GitLab CI](#gitlab-ci)
3. [Jenkins](#jenkins)
4. [Azure DevOps](#azure-devops)
5. [Failure Conditions](#failure-conditions)
6. [Best Practices](#best-practices)

---

## GitHub Actions

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
    runs-on: windows-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
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
      
      - name: Run Code Quality Analysis
        run: .\gradlew.bat analyzeCodeQuality
      
      - name: Upload Analysis Report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: code-quality-report
          path: .kiro/specs/code-quality-analysis/analysis-report.md
      
      - name: Check for Critical Issues
        run: |
          $report = Get-Content .kiro/specs/code-quality-analysis/analysis-report.md -Raw
          if ($report -match "CRITICAL") {
            Write-Error "Critical security issues found!"
            exit 1
          }
```

### Security-Focused Workflow

For security-focused analysis before releases:

```yaml
name: Security Analysis

on:
  pull_request:
    branches: [ main ]
  workflow_dispatch:

jobs:
  security-scan:
    runs-on: windows-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'oracle'
      
      - name: Run Security Analysis
        run: .\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=security,architecture
      
      - name: Check for Security Issues
        run: |
          $report = Get-Content .kiro/specs/code-quality-analysis/analysis-report.md -Raw
          
          # Check for CRITICAL issues
          if ($report -match "CRITICAL") {
            Write-Error "CRITICAL security issues found!"
            exit 1
          }
          
          # Check for HIGH issues
          if ($report -match "HIGH") {
            Write-Warning "HIGH priority issues found. Review required."
          }
      
      - name: Upload Security Report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: security-report
          path: .kiro/specs/code-quality-analysis/analysis-report.md
```

### Incremental Analysis for PRs

Analyze only changed files in pull requests:

```yaml
name: PR Code Quality

on:
  pull_request:
    branches: [ develop ]

jobs:
  analyze-changes:
    runs-on: windows-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'oracle'
      
      - name: Get changed files
        id: changed-files
        run: |
          $changedFiles = git diff --name-only origin/${{ github.base_ref }}...HEAD | Where-Object { $_ -match '\.kt$' }
          $paths = $changedFiles -join ','
          echo "paths=$paths" >> $env:GITHUB_OUTPUT
      
      - name: Run Incremental Analysis
        if: steps.changed-files.outputs.paths != ''
        run: |
          $paths = "${{ steps.changed-files.outputs.paths }}"
          .\gradlew.bat analyzeCodeQuality -Panalysis.path=$paths
      
      - name: Comment PR with Results
        uses: actions/github-script@v7
        if: always()
        with:
          script: |
            const fs = require('fs');
            const report = fs.readFileSync('.kiro/specs/code-quality-analysis/analysis-report.md', 'utf8');
            
            // Extract summary
            const summaryMatch = report.match(/## Executive Summary([\s\S]*?)##/);
            const summary = summaryMatch ? summaryMatch[1] : 'No summary available';
            
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `## Code Quality Analysis Results\n\n${summary}`
            });
```

### Baseline Tracking

Track code quality improvements over time:

```yaml
name: Weekly Quality Baseline

on:
  schedule:
    - cron: '0 0 * * 0'  # Every Sunday at midnight
  workflow_dispatch:

jobs:
  baseline:
    runs-on: windows-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'oracle'
      
      - name: Run Analysis and Update Baseline
        run: .\gradlew.bat analyzeCodeQuality -Panalysis.baseline=true
      
      - name: Commit Baseline
        run: |
          git config user.name "GitHub Actions"
          git config user.email "actions@github.com"
          git add .kiro/specs/code-quality-analysis/baseline.json
          git add .kiro/specs/code-quality-analysis/history/
          git commit -m "chore: update code quality baseline [skip ci]"
          git push
      
      - name: Create Issue if Quality Regressed
        uses: actions/github-script@v7
        if: failure()
        with:
          script: |
            github.rest.issues.create({
              owner: context.repo.owner,
              repo: context.repo.repo,
              title: 'Code Quality Regression Detected',
              body: 'The weekly code quality analysis detected a regression. Please review the latest report.',
              labels: ['quality', 'technical-debt']
            });
```

---

## GitLab CI

Create `.gitlab-ci.yml`:

```yaml
stages:
  - analyze
  - report

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"

code_quality:
  stage: analyze
  image: openjdk:17-jdk
  script:
    - ./gradlew analyzeCodeQuality
  artifacts:
    paths:
      - .kiro/specs/code-quality-analysis/analysis-report.md
    expire_in: 1 week
  only:
    - merge_requests
    - develop
    - main

security_scan:
  stage: analyze
  image: openjdk:17-jdk
  script:
    - ./gradlew analyzeCodeQuality -Panalysis.analyzers=security,architecture
    - |
      if grep -q "CRITICAL" .kiro/specs/code-quality-analysis/analysis-report.md; then
        echo "Critical security issues found!"
        exit 1
      fi
  artifacts:
    paths:
      - .kiro/specs/code-quality-analysis/analysis-report.md
    expire_in: 1 week
  only:
    - merge_requests
    - main

report_quality:
  stage: report
  image: alpine:latest
  script:
    - apk add --no-cache curl
    - |
      curl -X POST "$CI_API_V4_URL/projects/$CI_PROJECT_ID/issues" \
        --header "PRIVATE-TOKEN: $CI_JOB_TOKEN" \
        --form "title=Code Quality Report - $CI_COMMIT_SHORT_SHA" \
        --form "description=See attached report" \
        --form "labels=quality"
  dependencies:
    - code_quality
  only:
    - schedules
```

---

## Jenkins

Create `Jenkinsfile`:

```groovy
pipeline {
    agent any
    
    tools {
        jdk 'JDK 17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Code Quality Analysis') {
            steps {
                script {
                    if (isUnix()) {
                        sh './gradlew analyzeCodeQuality'
                    } else {
                        bat '.\\gradlew.bat analyzeCodeQuality'
                    }
                }
            }
        }
        
        stage('Check Results') {
            steps {
                script {
                    def report = readFile('.kiro/specs/code-quality-analysis/analysis-report.md')
                    
                    if (report.contains('CRITICAL')) {
                        error('Critical issues found in code quality analysis!')
                    }
                    
                    if (report.contains('HIGH')) {
                        unstable('High priority issues found. Review recommended.')
                    }
                }
            }
        }
        
        stage('Archive Report') {
            steps {
                archiveArtifacts artifacts: '.kiro/specs/code-quality-analysis/analysis-report.md', fingerprint: true
            }
        }
    }
    
    post {
        always {
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: '.kiro/specs/code-quality-analysis',
                reportFiles: 'analysis-report.md',
                reportName: 'Code Quality Report'
            ])
        }
        
        failure {
            emailext(
                subject: "Code Quality Analysis Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Check console output at ${env.BUILD_URL}",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
    }
}
```

---

## Azure DevOps

Create `azure-pipelines.yml`:

```yaml
trigger:
  branches:
    include:
      - develop
      - main

pr:
  branches:
    include:
      - develop
      - main

pool:
  vmImage: 'windows-latest'

variables:
  GRADLE_USER_HOME: $(Pipeline.Workspace)/.gradle

steps:
  - task: JavaToolInstaller@0
    inputs:
      versionSpec: '17'
      jdkArchitectureOption: 'x64'
      jdkSourceOption: 'PreInstalled'
  
  - task: Cache@2
    inputs:
      key: 'gradle | "$(Agent.OS)" | **/build.gradle.kts'
      restoreKeys: |
        gradle | "$(Agent.OS)"
        gradle
      path: $(GRADLE_USER_HOME)
    displayName: Cache Gradle packages
  
  - task: PowerShell@2
    displayName: 'Run Code Quality Analysis'
    inputs:
      targetType: 'inline'
      script: |
        .\gradlew.bat analyzeCodeQuality
  
  - task: PowerShell@2
    displayName: 'Check for Critical Issues'
    inputs:
      targetType: 'inline'
      script: |
        $report = Get-Content .kiro/specs/code-quality-analysis/analysis-report.md -Raw
        
        if ($report -match "CRITICAL") {
          Write-Host "##vso[task.logissue type=error]Critical issues found!"
          Write-Host "##vso[task.complete result=Failed;]"
        }
        
        if ($report -match "HIGH") {
          Write-Host "##vso[task.logissue type=warning]High priority issues found!"
        }
  
  - task: PublishBuildArtifacts@1
    displayName: 'Publish Analysis Report'
    condition: always()
    inputs:
      PathtoPublish: '.kiro/specs/code-quality-analysis/analysis-report.md'
      ArtifactName: 'code-quality-report'
```

---

## Failure Conditions

### Recommended Failure Criteria

#### Block Merge (Fail Build)
- **CRITICAL** priority issues found
- Security vulnerabilities detected
- Architecture violations in main/develop branches

#### Warning (Unstable Build)
- **HIGH** priority issues found
- Significant increase in issues compared to baseline
- Test coverage below threshold

#### Pass with Notice
- **MEDIUM** and **LOW** priority issues
- Minor improvements suggested
- Documentation gaps

### Implementation Examples

#### GitHub Actions

```yaml
- name: Evaluate Results
  run: |
    $report = Get-Content .kiro/specs/code-quality-analysis/analysis-report.md -Raw
    
    # Count issues by priority
    $criticalCount = ([regex]::Matches($report, "CRITICAL")).Count
    $highCount = ([regex]::Matches($report, "HIGH")).Count
    
    Write-Host "Found $criticalCount CRITICAL and $highCount HIGH issues"
    
    # Fail on CRITICAL
    if ($criticalCount -gt 0) {
      Write-Error "Build failed: $criticalCount CRITICAL issues found"
      exit 1
    }
    
    # Warn on HIGH
    if ($highCount -gt 5) {
      Write-Warning "Build unstable: $highCount HIGH issues found"
      exit 0
    }
```

#### Jenkins

```groovy
stage('Evaluate Quality') {
    steps {
        script {
            def report = readFile('.kiro/specs/code-quality-analysis/analysis-report.md')
            
            def criticalCount = (report =~ /CRITICAL/).count
            def highCount = (report =~ /HIGH/).count
            
            if (criticalCount > 0) {
                error("Build failed: ${criticalCount} CRITICAL issues found")
            }
            
            if (highCount > 5) {
                unstable("Build unstable: ${highCount} HIGH issues found")
            }
        }
    }
}
```

---

## Best Practices

### 1. Run Different Analyses at Different Stages

```yaml
# On every commit
- Run: Security + Architecture analyzers
- Fail on: CRITICAL issues

# On PR
- Run: All analyzers
- Fail on: CRITICAL issues
- Warn on: HIGH issues

# Before release
- Run: All analyzers + Detekt
- Fail on: CRITICAL or HIGH issues
- Generate: Baseline

# Weekly
- Run: Full analysis
- Update: Baseline
- Track: Trends
```

### 2. Cache Gradle Dependencies

Always cache Gradle dependencies to speed up builds:

```yaml
- uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
```

### 3. Parallel Execution

Run different analyzer groups in parallel:

```yaml
jobs:
  security:
    runs-on: windows-latest
    steps:
      - run: .\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=security
  
  architecture:
    runs-on: windows-latest
    steps:
      - run: .\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=architecture
  
  performance:
    runs-on: windows-latest
    steps:
      - run: .\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=performance
```

### 4. Incremental Analysis for PRs

Only analyze changed files in pull requests:

```yaml
- name: Get changed Kotlin files
  run: |
    git diff --name-only origin/develop...HEAD | grep '\.kt$' > changed_files.txt

- name: Analyze changed files
  run: |
    $files = Get-Content changed_files.txt
    if ($files) {
      $paths = $files -join ','
      .\gradlew.bat analyzeCodeQuality -Panalysis.path=$paths
    }
```

### 5. Baseline Tracking

Track improvements over time:

```yaml
# Weekly baseline update
- schedule:
    - cron: '0 0 * * 0'

- run: .\gradlew.bat analyzeCodeQuality -Panalysis.baseline=true

- name: Commit baseline
  run: |
    git add .kiro/specs/code-quality-analysis/baseline.json
    git commit -m "chore: update quality baseline"
    git push
```

### 6. Report Artifacts

Always save reports as artifacts:

```yaml
- uses: actions/upload-artifact@v3
  if: always()
  with:
    name: code-quality-report
    path: .kiro/specs/code-quality-analysis/analysis-report.md
    retention-days: 30
```

### 7. Notifications

Notify team of quality issues:

```yaml
- name: Notify on Slack
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "Code quality analysis failed for ${{ github.repository }}",
        "blocks": [
          {
            "type": "section",
            "text": {
              "type": "mrkdwn",
              "text": "Critical issues found in <${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}|build #${{ github.run_number }}>"
            }
          }
        ]
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

---

## Troubleshooting

### Build Timeout

If analysis takes too long:

```yaml
# Increase timeout
timeout-minutes: 30

# Or run fewer analyzers
-Panalysis.analyzers=security,architecture
```

### Out of Memory

Increase Gradle heap size:

```yaml
env:
  GRADLE_OPTS: -Xmx4096m -XX:MaxMetaspaceSize=1024m
```

### Windows Path Issues

Use proper path separators:

```yaml
# PowerShell
$paths = "app\src\main\java"

# Or use forward slashes
$paths = "app/src/main/java"
```

---

## Summary

Integrate code quality analysis into your CI/CD pipeline to:

1. **Catch issues early** - Before they reach production
2. **Enforce standards** - Automatically check code quality
3. **Track improvements** - Monitor quality trends over time
4. **Prevent regressions** - Block merges with critical issues
5. **Educate team** - Provide actionable feedback

Choose the appropriate failure conditions and analysis frequency based on your team's needs and release cycle.
