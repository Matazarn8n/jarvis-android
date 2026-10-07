---
name: android-intent-security
description: Best practices for Android Intent security. Use this skill when auditing
  component configurations in AndroidManifest.xml activities, services, receivers)
  or source code handling incoming Intents (getIntent, getParcelableExtra) to prevent
  Intent Redirection and unauthorized access.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-08-14'
  keywords:
  - recipe
  - Android
  - Security
  - Intent
  - Redirection
  - PendingIntent
  - ContentProvider
  - Service
  - Signature
  - Verification
  - sanitizer
  - Vulnerability
  - Best Practices
---
## Contents

- Glossary
- Prerequisites
- Limitations
- Setup and dependencies
- Intent security logic and decisions
- Code and configuration patterns
- Error handling
- Reporting guidelines
- Antipatterns
- Best Practices


This skill provides guidelines and patterns to secure Android components
(Activities, Services, Broadcast Receivers, Content Providers) and handle
Intents safely, preventing privilege escalation and unauthorized access.

## Glossary

- **Intent:** An asynchronous messaging object used to request an action from another app component.
- **Exported component:** A component (`android:exported="true"`) that can be launched by other apps on the device.
- **Sticky intent:** A broadcast intent that remains in the system cache after it's sent, allowing any app to retrieve its contents.
- **Signature permission:** A permission whose protection level is set to `signature`, granted only to apps signed with the same developer key.
- **onNewIntent:** An activity lifecycle callback invoked when an activity is launched with `FLAG_ACTIVITY_SINGLE_TOP` and is already running at the top of the history stack.
- **PendingIntent:** A token granted to a foreign application (for example, system services) allowing it to execute a predefined Intent with the creator's permissions.
- **Mutable PendingIntent:** A PendingIntent whose underlying Intent parameters can be modified by the receiving application.
- **ContentProvider:** A component that encapsulates data and provides it to other applications via standard query/insert interfaces.
- **IntentSanitizer:** A utility class in AndroidX Core used to build a safe, sanitized copy of an incoming Intent by filtering out unauthorized components, actions, or extras.
- **Intent redirection (forwarding):** A vulnerability where an application receives an intent from an untrusted source and uses it to launch a private, non-exported component.

## Prerequisites

- The agent **MUST** be able to describe the function and security implications of `onCreate`, `onNewIntent`, and the `singleTop` launch mode.
- The agent **MUST** be able to declare `<activity>`, `<service>`, `<receiver>`, and `<provider>` tags in `AndroidManifest.xml` and define their `android:exported` and `android:permission` attributes.
- The agent **MUST** be able to implement signature verification checks using `PackageManager`.

## Limitations

- This skill focuses on local inter-component and inter-app communication security on the Android platform.
- This skill **doesn't** cover network security, web integration, or host-to-server security.

## Setup and dependencies

- **Android SDK:** Minimum API Level 23 (Android 6.0) is required for standard hardware-backed keystore operations and component validation.
- **AndroidX Core Library:** `androidx.core:core:1.9.0` or higher is **mandatory** to leverage `IntentSanitizer`.
- **Standard API access:** Standard Android `PackageManager` APIs are required for runtime component verification.

*** ** * ** ***

## Intent security logic and decisions

### 1. Intent routing comparison

Evaluate the security features of different intent delivery methods:

| Intent Delivery Method | Scope | Recommended Use Case |
|---|---|---|
| Explicit Intent (Internal) | App Private | Launching internal activities/services |
| Implicit Intent | System Wide | Launching system camera, dialer, or sharing |
| Local Broadcasts (LocalBroadcastManager) (DEPRECATED) | App Private | Internal asynchronous event routing. **Deprecated**: Use in-app observers like Kotlin Flows/SharedFlow, LiveData, or reactive patterns instead. |
| System Broadcasts | System Wide | Receiving system events (NFC, Bluetooth) |

### 2. PendingIntent mutability flag options

Evaluate the security implications of PendingIntent mutability flags:

| Flag Name | Mutability | Recommended Use Case |
|---|---|---|
| `PendingIntent.FLAG_IMMUTABLE` | Immutable | Default for almost all PendingIntents, such as alarms and notifications |
| `PendingIntent.FLAG_MUTABLE` | Mutable | Inline notifications replies, slice actions (requires explicit target intent) |

### 3. Intent handling and redirection logic

IF (the component receives a nested Intent as an extra) {
IF (AndroidX Core 1.9.0+ and higher is available) {
MUST construct an `IntentSanitizer` to explicitly allowlist components, actions, data, and extras.
MUST call `sanitizeByThrowing()` or `sanitizeByFiltering()` before launching.
} ELSE {
MUST verify that the nested Intent's target package matches the current application package.
MUST verify that the target component of the nested Intent is publicly exported.
}
NEVER launch the nested Intent directly without validation.
} ELSE IF (the component handles broadcasts) {
MUST rely on the system's Protected Broadcast mechanism for system events (which guarantees the sender is the system framework).
MUST protect custom receivers with signature-level permissions or use `RECEIVER_NOT_EXPORTED` for dynamic receivers to restrict the sender.
}

### 4. PendingIntent security logic

IF (a PendingIntent is created for delivery to another application) {
MUST use `PendingIntent.FLAG_IMMUTABLE` by default.
IF (the PendingIntent must be mutable) {
MUST set the explicit target component or package name on the base `Intent`.
NEVER create an implicit, mutable `PendingIntent`.
}
}

### 5. ContentProvider security logic

IF (the ContentProvider is only for internal app use) {
MUST set `android:exported="false"`.
} ELSE {
MUST protect it with `android:readPermission` and `android:writePermission`.
MUST set `android:grantUriPermissions="false"` unless temporary URL access is strictly required.
}

### 6. Service caller verification logic

IF (an exported service communicates with trusted sister/partner apps) {
MUST retrieve the calling UID using `Binder.getCallingUid()` and resolve it to package names using `PackageManager.getPackagesForUid()`.
MUST verify that the calling package signature fingerprint matches your trusted certificate hash.
}

*** ** * ** ***

## Code and configuration patterns

Seven annotated patterns (safe intent redirection, IntentSanitizer, signature permission, onNewIntent, secure PendingIntent, ContentProvider, service caller verification) live in [references/code-patterns.md](references/code-patterns.md). Read it before writing or reviewing code.

## Error handling

Handle component binding, database queries, and intent redirection failures
securely to avoid exposing internal structures.


```kotlin
fun safeErrorHandling(callingPackage: String?) {
    try {
        val payload = intent.getStringExtra("DATA_EXTRA") ?: throw IllegalArgumentException("Payload parameter missing.")
        // Create a specific target intent using the validated payload
        val targetIntent = Intent(this, TargetActivity::class.java).apply {
            putExtra("SECURE_PAYLOAD", payload)
        }
        startActivity(targetIntent)
    } catch (e: SecurityException) {
        // MUST log security violations for audit, but NEVER expose exception details to the user.
        Log.e("SECURITY_ERROR", "Unauthorized component transition blocked. Calling Package: ${callingPackage ?: "Unknown"}", e)
        // MUST provide generic user feedback.
        showFeedbackToUser("Process request failed: Access Denied.")
    } catch (e: IllegalArgumentException) {
        Log.w("INTEGRITY_WARNING", "Missing intent parameter", e)
    }
}
```

<br />

    // Secure handling of ContentProvider queries on the client side:
    try {
        val cursor = contentResolver.query(providerUri, projection, selection, selectionArgs, null)
    } catch (e: SQLiteException) {
        Log.e("PROVIDER_ERROR", "ContentProvider database query failed", e)
        // Secure handling: prevent raw query syntax details from leaking to UI
    }

*** ** * ** ***

## Reporting guidelines

When this skill is executed to apply security hardening updates to a codebase,
the agent **MUST** generate a structured "Best Practices and Security Alignment
Update" report for the developer. The report **must** be written to the session
artifact folder (or printed in the final response) and include:

1. **Security alignment area:** The category of improvement applied (for example, Safe Intent Redirection, Secure PendingIntent Configuration, ContentProvider Data Guarding).
2. **Impact and priority:** The potential safety risk addressed by the update (for example, Component Hijacking Prevention, Private Data Isolation).
3. **Scope of changes:** A list of all modified classes, XML files, and dependencies.
4. **Implementation summary:** Concrete details of the solution (for example, "Updated nested intent parsing to use the `IntentSanitizer` API with a strict component allowlist").
5. **Code diff:** Standard unified diffs showing the exact modifications.

### Best practices and security alignment update template

Use the following markdown template when reporting changes to developers:

    ### Best practices and security alignment update: [Security Alignment Area]

    *   **Improvement Description:** [Brief description of the hardening update and why it's recommended]
    *   **Priority Level:** [High / Medium / Low]
    *   **Alignment Action:** [Summary of updates, for example, converted to FLAG_IMMUTABLE]

    #### Files modified
    *   `[Relative path to File 1]`
    *   `[Relative path to File 2]`

    #### Implementation diff
    ```diff
    // Insert Unified Diff here

#### Testing and verification

1. \[Step 1 to verify the component behaves correctly, for example, run component unit test\]
2. \[Step 2 to verify regression safety\] \`\`\`

*** ** * ** ***

## Antipatterns

- **NEVER** launch a nested `Intent` received from an untrusted source without verifying its target package and exported status.
- **NEVER** use sticky broadcasts (`sendStickyBroadcast`).
- **NEVER** assume an exported component is safe because it runs in a background thread or performs internal checks.
- **NEVER** expose sensitive functionalities (like SSO authentication or payment processors) to components without signature-level permission restrictions.
- **NEVER** process incoming intents in `onNewIntent` without applying the same security controls as `onCreate`.
- **NEVER** create a mutable `PendingIntent` without setting an explicit target component in the base `Intent`.
- **NEVER** use dynamic string concatenation to construct selection blocks inside a `ContentProvider` query.
- **NEVER** use `Binder.getCallingUid` inside a `BroadcastReceiver.onReceive` to identify the sender of a broadcast, as it returns the receiver's own UID, not the sender's.

## Best Practices

- **MUST** explicitly set `android:exported="false"` for all components that don't need external communication.
- **MUST** protect all exported components with custom permissions utilizing `android:protectionLevel="signature"` when communicating between family apps.
- **MUST** validate all incoming intent extras and handle missing parameters gracefully to prevent crashes.
- **MUST** rely on the system's **Protected Broadcast** mechanism for system events (for example, boot completed, package changes), as the system prevents untrusted apps from spoofing these actions.
- **MUST** protect custom broadcasts with signature-level permissions or use `RECEIVER_NOT_EXPORTED` for dynamic receivers to restrict the sender identity.
- **MUST** call `setIntent(newIntent)` inside `onNewIntent()` before processing payloads to keep active references updated.
- **MUST** use `PendingIntent.FLAG_IMMUTABLE` by default when constructing `PendingIntent` instances.
- **MUST** protect exported `ContentProviders` with `readPermission` and `writePermission`.
- **MUST** enforce parameterized selection structures in `ContentProvider` query/update methods.
- **MUST** verify the package signature fingerprint of binding applications at runtime inside exported services.
- **MUST** use `androidx.core.content.IntentSanitizer` to sanitize incoming dynamic intents before redirection, if AndroidX Core 1.9.0+ is imported in the project.
