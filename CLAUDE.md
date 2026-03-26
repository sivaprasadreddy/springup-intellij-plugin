# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

An IntelliJ IDEA plugin that improves developer productivity while building Spring Boot applications.

* **Generate CRUD**: Right-click a JPA entity class and select **New → Generate CRUD** to scaffold a full CRUD REST API layer in one step.

## Build & Run Commands

```bash
./gradlew build          # Compile and build
./gradlew runIde         # Launch IntelliJ sandbox with plugin loaded
./gradlew buildPlugin    # Build distributable plugin ZIP
./gradlew verifyPlugin   # Verify IDE compatibility
./gradlew test           # Run tests
./gradlew clean          # Clean build outputs
```

## Key Configuration Files

- `build.gradle.kts` — Uses `org.jetbrains.intellij.platform` v2.13.1, targets IntelliJ 2025.1.7 (build 251+), Java 21 toolchain.
- `src/main/resources/META-INF/plugin.xml` — Plugin descriptor with action and extension point registration.
