# Code and configuration patterns


### 1. Safe intent redirection (manual verification)

Validate the target of a nested intent before launching it when modern
sanitization libraries are unavailable.

- **Expected Inputs:**
  - An incoming `Intent` containing a nested `Intent` extra named `EXTRA_NESTED_INTENT`.
- **Expected Outputs:**
  - Launches the target component if safe; throws `SecurityException` if validation fails.


```kotlin
fun safeIntentRedirectionManual() {
    val nestedIntent = IntentCompat.getParcelableExtra(intent, "EXTRA_NESTED_INTENT", Intent::class.java)
    if (nestedIntent != null) {
        // 1. Check for URI permission grants to prevent URI permission bypass
        val hasUriPermissionGrants = (
            nestedIntent.flags and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                )
            ) != 0
        if (hasUriPermissionGrants) {
            throw SecurityException("Nested intent contains forbidden URI permission grant flags!")
        }

        val pm = packageManager
        val target = nestedIntent.resolveActivity(pm)
        if (target != null) {
            // 2. Verify target is within the same package
            if (target.packageName != packageName) {
                throw SecurityException("Cross-app intent redirection is forbidden!")
            }
            try {
                // 3. Verify target activity is exported
                val info = pm.getActivityInfo(target, 0)
                if (!info.exported) {
                    throw SecurityException("Target activity is private: ${target.className}")
                }
                // 4. Explicitly set the component to prevent intent interception
                nestedIntent.component = target
                // Safe to launch
                startActivity(nestedIntent)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("Security", "Failed to resolve target activity", e)
            }
        }
    }
}
```

<br />

### 2. Safe intent redirection using IntentSanitizer

Filter or reject dynamic intents using AndroidX `IntentSanitizer` (AndroidX Core
1.9.0+).

- **Expected Inputs:**
  - An untrusted incoming `Intent`.
- **Expected Outputs:**
  - `Intent`: A sanitized copy containing only allowlisted components, categories, and actions. Throws `SecurityException` on violations if using `sanitizeByThrowing()`.


```kotlin
fun safeIntentRedirectionSanitizer() {
    val untrustedIntent = IntentCompat.getParcelableExtra(intent, "EXTRA_NESTED_INTENT", Intent::class.java)
    if (untrustedIntent != null) {
        // Define the strict boundaries for allowed redirection target
        val sanitizer = IntentSanitizer.Builder()
            .allowComponent(ComponentName("com.example.app", "com.example.app.SafeTargetActivity")) // Explicitly allowed target
            .allowAction(Intent.ACTION_VIEW) // Explicitly allowed actions
            .allowDataWithAuthority("com.example.app.provider") // Allowed URI authority
            .allowType("text/plain") // Allowed mime type
            .allowExtra("user_display_name", String::class.java) // Safe type-enforced extras
            // Note: URI permission flags are NOT allowed, so the sanitizer will automatically strip or throw on them
            .build()

        try {
            // Option A: Throws SecurityException if the intent violates policies
            val safeIntent = sanitizer.sanitizeByThrowing(untrustedIntent)
            startActivity(safeIntent)
        } catch (e: SecurityException) {
            Log.e("SECURITY_ALERT", "Attempted launch of non-allowlisted intent blocked", e)
        }

        // Option B: Silently filter and launch only the authorized parts (no exception thrown)
        // val filteredIntent = sanitizer.sanitizeByFiltering(untrustedIntent)
        // startActivity(filteredIntent)
    }
}
```

<br />

### 3. Custom signature permission protection

Declare a custom signature-level permission in the manifest to secure family app
communication.

- **Expected Inputs:** Manifest configuration.
- **Expected Outputs:** An activity that can only be launched by apps signed with the same developer certificate.


```xml
<permission
    android:name="com.example.snippets.permission.INTERNAL_COMMUNICATION"
    android:protectionLevel="signature" />
```

<br />


```xml
<activity
    android:name=".intents.InternalSharingActivity"
    android:exported="true"
    android:permission="com.example.snippets.permission.INTERNAL_COMMUNICATION">
    <intent-filter>
        <action android:name="com.example.snippets.ACTION_SHARE" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

<br />

### 4. Safe onNewIntent lifecycle verification (warm boot protection)

Ensure that activities reusing dynamic intents (for example, in background
launch paths) apply the same strict security filters inside `onNewIntent`.

- **Expected Inputs:**
  - `newIntent` (`Intent`): The newly delivered intent.
- **Expected Outputs:**
  - Executes processing logic only if the new intent passes security validation.


```kotlin
override fun onNewIntent(newIntent: Intent) {
    super.onNewIntent(newIntent)

    // Set the intent to ensure intent returns the new one
    intent = newIntent

    // Validate the intent payload
    if (validateIntent(newIntent)) {
        processIntentPayload(newIntent)
    } else {
        Log.w("SECURITY_ALERT", "Received invalid or insecure intent during warm boot")
    }
}

private fun validateIntent(intent: Intent): Boolean {
    return intent.hasExtra("VALID_PAYLOAD_MARKER")
}
```

<br />

### 5. Secure PendingIntent creation

Enforce immutability unless mutability is explicitly required.

- **Expected Inputs (Immutable):** An intent target.
- **Expected Outputs (Immutable):** A `PendingIntent` that cannot be altered by the receiver.
- **Expected Inputs (Mutable):** An intent with an explicit component set.
- **Expected Outputs (Mutable):** A mutable `PendingIntent` locked to a specific receiver component to prevent hijacking.


```kotlin
fun createPendingIntents(context: Context) {
    // 1. Secure Immutable PendingIntent (Default)
    val intent = Intent(context, TargetActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // 2. Secure Mutable PendingIntent (e.g., Notification Direct Reply)
    val mutableIntent = Intent().apply {
        // MUST set explicit target component to prevent redirection hijacking
        component = ComponentName(context, ReplyReceiver::class.java)
    }
    val mutablePendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        mutableIntent,
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
```

<br />

### 6. Secure ContentProvider configuration and queries

Expose a ContentProvider securely and parameterize queries to prevent SQL
injection.

- **Expected Inputs:**
  - `uri` (`Uri`): The query URI.
  - `projection` (`String[]`): Columns to retrieve.
  - `selection` (`String`): Query criteria.
  - `selectionArgs` (`String[]`): Values mapping to selection placeholders (`?`).
- **Expected Outputs:**
  - `Cursor`: Filtered query results, strictly bound to projection maps.


```xml
<provider
    android:name=".intents.SecureDataProvider"
    android:authorities="com.example.snippets.provider"
    android:exported="true"
    android:readPermission="com.example.snippets.permission.READ_DATA"
    android:writePermission="com.example.snippets.permission.WRITE_DATA"
    android:grantUriPermissions="false" />
```

<br />


```kotlin
override fun query(
    uri: Uri,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?
): Cursor? {
    val queryBuilder = SQLiteQueryBuilder()
    queryBuilder.tables = tableName
    // Strict projection map to prevent querying unauthorized columns
    queryBuilder.projectionMap = mapOf(
        "_id" to "_id",
        "display_name" to "display_name"
    )
    // Enable strict validation (always available since minSdk is 36)
    queryBuilder.setStrict(true)
    queryBuilder.setStrictColumns(true)
    queryBuilder.setStrictGrammar(true)

    // MUST parameterize selection criteria; NEVER append selection strings directly
    val db = dbHelper.readableDatabase
    return queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder)
}
```

<br />

### 7. Service caller signature verification

Verify the calling application's signature before binding to a service.

- **Expected Inputs:**
  - `intent` (`Intent`): The binding request intent.
- **Expected Outputs:**
  - `IBinder`: Local binder instance if caller signature matches trusted partner; throws `SecurityException` otherwise.


```kotlin
class SecureBoundService : Service() {
    companion object {
        // Expected SHA-256 hash of the trusted app's signing certificate (Base64 encoded)
        private const val TRUSTED_PARTNER_SHA256 = "A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V="
    }

    override fun onBind(intent: Intent): IBinder {
        // Return the binder. Do NOT perform signature verification in onBind() because
        // the binder connection is cached by Android, which can bypass checks on subsequent binds.
        return LocalBinder()
    }

    private fun enforceTrustedCaller() {
        val callingUid = Binder.getCallingUid()
        // Allow calls from the same application
        if (callingUid == Process.myUid()) {
            return
        }
        val pm = packageManager
        val packages = pm.getPackagesForUid(callingUid)

        if (packages.isNullOrEmpty() || !verifySignature(pm, packages[0])) {
            throw SecurityException("Access Denied: Caller signature is untrusted.")
        }
    }

    private fun verifySignature(pm: PackageManager, packageName: String): Boolean {
        try {
            val trustedSha256Raw = Base64.decode(TRUSTED_PARTNER_SHA256, Base64.DEFAULT)
            // API 28+ handles rotated certificates and avoids manual hashing.
            // Since minSdk is 36, this is always available.
            return pm.hasSigningCertificate(packageName, trustedSha256Raw, PackageManager.CERT_INPUT_SHA256)
        } catch (e: Exception) {
            Log.e("SECURITY_ERROR", "Verification failed for package: $packageName", e)
        }
        return false
    }

    inner class LocalBinder : Binder() {
        fun doSecureWork() {
            // Verify caller identity on every transaction method call
            enforceTrustedCaller()
            // Safe to proceed with sensitive operations
        }
    }
}
```

<br />

*** ** * ** ***
