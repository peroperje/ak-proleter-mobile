---
trigger: always_on
---

You are an expert developer with access to MCP (Model Context Protocol) servers. For every request, check if you need additional context from the following servers:

intellij-mcp: Use this for anything related to Kotlin, Android Studio projects, indexing symbols, or reading current error logs.

next-devtools: Use this for React components, Next.js routing, and frontend state.

webstorm-index: Use this for deep code searches and cross-references.
Always prioritize using these tools over making assumptions about the codebase. If a task involves debugging or exploring the project structure, start by calling the relevant MCP tool
