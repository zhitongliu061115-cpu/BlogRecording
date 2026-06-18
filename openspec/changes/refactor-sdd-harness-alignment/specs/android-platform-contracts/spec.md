## ADDED Requirements

### Requirement: Android identity remains stable
The app SHALL keep Gradle namespace, applicationId, and Kotlin package identity as `com.example.blogrecording`.

#### Scenario: Debug build is produced
- **WHEN** `assembleDebug` runs
- **THEN** the app identity remains `com.example.blogrecording`

### Requirement: Launcher and service declarations remain stable
The app SHALL keep `MainActivity` exported as the launcher activity and `CaptureForegroundService` exported false with microphone and mediaProjection foreground service types.

#### Scenario: Manifest is inspected
- **WHEN** the manifest is read
- **THEN** launcher Activity and foreground service declarations match the existing contract

### Requirement: Platform permissions remain declared
The app SHALL keep existing permissions for recording audio, internet, notifications, foreground service, microphone foreground service, and media projection foreground service.

#### Scenario: App installs on supported Android versions
- **WHEN** the app is installed
- **THEN** the required platform permissions are declared for current flows

### Requirement: Backup and data extraction rules remain configured
The app SHALL keep `android:allowBackup="true"` and the existing backup and data extraction XML resources referenced by the manifest until a separate privacy change modifies backup policy.

#### Scenario: Manifest references backup rules
- **WHEN** the manifest is validated
- **THEN** backup and data extraction rule resources exist

#### Scenario: Backup policy is unchanged
- **WHEN** the rebuild updates platform contracts
- **THEN** it does not silently change the current backup/data extraction XML include or exclude behavior

### Requirement: Foreground notification contract remains stable
The app SHALL keep foreground capture notification channel id `capture_foreground`, notification id `1001`, extra key `foreground_service_type`, and immediate foreground-service notification behavior.

#### Scenario: Capture foreground service starts
- **WHEN** the service receives a foreground service type extra
- **THEN** it starts with notification id `1001` on channel `capture_foreground`
