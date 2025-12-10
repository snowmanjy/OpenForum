---
description: Standard protocol for implementing code changes. Enforces strict verification and server management rules.
---

# Standard Change Protocol

Follow this protocol for EVERY code change task.

1. **Implement Changes**
    - Modify code as required.
    - Ensure code compiles `mvn clean install -DskipTests ...` (using `-pl` for speed).

2. **Verify with Assertions (CRITICAL)**
    - **NEVER** just check for `status().isOk()`.
    - **ALWAYS** add `jsonPath` assertions for specific data values.
    - **Tests MUST verify the actual code changes** - not just that the endpoint works.
    - Example: If you add a `score` field, assert `jsonPath("$.score").value(42)` with a known test value.
    - Example:

      ```java
      .andExpect(jsonPath("$.field").value("expectedValue"))
      .andExpect(jsonPath("$.id").isNotEmpty())
      ```

    - Run the specific test: `mvn test -pl <module> -Dtest=<TestName>`.

3. **Restart Server (CRITICAL)**
    - **ALWAYS** restart the server after verifying changes.
    - **DO NOT ASK** for permission. Just do it.
    - Command:

      ```bash
      # Find and kill old process
      ps aux | grep spring-boot
      kill <PID>
      # Start new process
      mvn spring-boot:run -pl forum-boot
      ```

// turbo-all
