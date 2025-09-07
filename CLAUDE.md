# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Processing-based Java application for an AI-powered coding agent that enables users to interact with an AI to generate and execute visual drawings through natural language instructions.

## Architecture

The system follows a layered architecture:
- **Presentation Layer**: UI components for drawing canvas, chat interface, and approval workflows
- **Application Layer**: Agent services, session management, and use cases
- **Domain Layer**: Core models, tools, services, and repositories  
- **Infrastructure Layer**: AI client integration, file storage, HTTP communication

Main entry point: `com.example.SimpleSketch` - A Processing sketch that provides the drawing canvas and UI areas.

## Build and Run Commands

### Compile the project
```bash
mvn compile
```

### Run the application
```bash
mvn exec:java
```
The main class is configured in `pom.xml` as `com.example.SimpleSketch`.

### Run with a different main class
```bash
mvn exec:java -Dexec.mainClass="com.example.OtherClass"
```

### Run tests
```bash
mvn test
```

### Build JAR package
```bash
mvn package
```

### Clean build artifacts
```bash
mvn clean
```

## Technology Stack

- **Java 17** (Amazon Corretto)
- **Processing 4.4.7** - Graphics and visualization library
- **JOGAMP 2.5.0** - OpenGL support for Processing
- **Maven 3.9.x** - Build and dependency management
- **JUnit 5** - Testing framework

## Key Dependencies

The project uses Processing Core with explicit JOGAMP dependencies to handle OpenGL rendering. Font issues with Japanese text are handled by explicitly creating fonts using `createFont("MS Gothic", 24, true)`.

## Common Development Tasks

### Handling Font Warnings
If you see "Some characters not available" warnings, ensure fonts are properly initialized in the `setup()` method using `createFont()` and `textFont()`.

### Running with JVM Options
```bash
mvn exec:java -Dexec.jvmArgs="-Xms256m -Xmx512m"
```

### Debug Mode
```bash
mvn exec:java -Dexec.jvmArgs="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

## Project Structure

- `src/main/java/com/example/` - Main application code
  - `SimpleSketch.java` - Processing sketch with drawing canvas
- `docs/` - Documentation
  - `RUNNING.md` - Detailed execution guide and troubleshooting
  - `command.md` - Maven command reference
- `requirement/` - Requirements and architecture documents
  - `prd.md` - Product requirements document
  - `architecture.md` - System architecture design

## AI Agent Integration

The system is designed to integrate with Claude API or Gemini API for natural language processing. The agent interprets user instructions and generates YAML-based drawing instructions that require user approval before execution.

## Important Notes

- The application requires user approval for all drawing changes proposed by the AI agent
- Drawing instructions are structured in YAML format for clarity and safety
- The system maintains state history for undo/redo functionality
- Session data can be saved and loaded for continuity

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md
