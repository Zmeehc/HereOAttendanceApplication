# Login Security System

This document describes the login security system implemented in the HereOAttendance Android application.

## Overview

The login security system provides protection against unauthorized access by implementing:
- Failed login attempt tracking
- SMS security alerts after 3 failed attempts
- No account lockout - users can continue attempting to login
- Immediate notification to account owners when suspicious activity is detected

## Features

### 1. Failed Attempt Counter
- Tracks failed login attempts per user (identified by email)
- Counter persists across app sessions using SharedPreferences
- Counter resets on successful login

### 2. SMS Security Alerts
- Sends SMS message "Someone is trying to login into your account" after 3 failed attempts
- SMS sent to the phone number registered in the user's profile
- Uses SemaphoreSmsSender API for SMS delivery
- **No account lockout** - users can continue attempting to login

### 3. User Experience
- Clear feedback on remaining login attempts before SMS alert
- Informative messages about security alerts
- Loading states during login attempts
- Toast notifications for all security events

## Implementation Details

### Files Modified/Created

1. **TeacherLoginActivity.java** - Main login activity with security integration
2. **LoginSecurityManager.java** - Utility class for managing security features
3. **SemaphoreSmsSender.java** - SMS sending functionality (already existed)

### Key Classes

#### LoginSecurityManager
- Manages failed attempt counters
- Determines when SMS alerts should be sent
- Provides utility methods for security operations
- Configurable security parameters

#### TeacherLoginActivity
- Integrates with LoginSecurityManager
- Handles login attempts and security responses
- Manages UI state during security events
- Sends security alerts via SMS

### Security Parameters

- **Max Failed Attempts**: 3 (configurable)
- **SMS Alert Trigger**: After 3 failed attempts
- **Storage**: SharedPreferences with encrypted keys
- **User Identification**: Email address
- **Account Lockout**: None - users can continue attempting to login

## Usage

### Basic Login Flow
1. User enters email and password
2. System attempts authentication
3. On success: resets counters, proceeds to dashboard
4. On failure: increments counter, shows remaining attempts
5. After 3 failures: sends SMS alert, user can continue trying

### Admin Functions
- `resetLoginAttemptsManually()` - Reset failed attempts
- `getCurrentFailedAttempts()` - Get current attempt count
- `shouldSendSmsAlert()` - Check if SMS alert should be sent

## Configuration

### Customizing Security Settings
```java
// Custom security settings
LoginSecurityManager securityManager = new LoginSecurityManager(
    context, 
    userEmail, 
    5                    // Max attempts before SMS alert
);

// Default settings (3 attempts before SMS alert)
LoginSecurityManager securityManager = new LoginSecurityManager(context, userEmail);
```

### SMS Configuration
- SMS API key configured in `SemaphoreSmsSender.java`
- Sender name: "FarmBite"
- Endpoint: Semaphore SMS API

## Security Considerations

1. **Data Persistence**: Security data stored in SharedPreferences
2. **User Identification**: Uses email as unique identifier
3. **SMS Verification**: Alerts users of suspicious activity
4. **Automatic Reset**: Counters reset on successful login
5. **No Lockout**: Users can continue attempting to login
6. **Immediate Alert**: SMS sent immediately after 3rd failed attempt

## Future Enhancements

1. **Student Login Security**: Extend to student login activities
2. **Admin Panel**: Web interface for monitoring failed attempts
3. **Advanced Analytics**: Track login patterns and suspicious activity
4. **Two-Factor Authentication**: Additional security layer
5. **IP-based Monitoring**: Track suspicious IP addresses

## Testing

### Test Scenarios
1. **Normal Login**: Valid credentials should work
2. **Failed Login**: Counter should increment
3. **SMS Alert**: Should send SMS after 3 failures
4. **Counter Reset**: Should reset on successful login
5. **Continued Attempts**: User should be able to keep trying after SMS alert

### Test Data
- Use test Firebase accounts
- Verify SMS delivery
- Check SharedPreferences storage
- Monitor logcat for security events

## Troubleshooting

### Common Issues
1. **SMS Not Sending**: Check API key and network connectivity
2. **Counter Not Resetting**: Verify SharedPreferences operations
3. **SMS Alert Not Triggering**: Check attempt counting logic
4. **UI Not Updating**: Ensure runOnUiThread usage

### Debug Information
- Logcat tags: "TeacherLoginActivity", "LoginSecurityManager"
- SharedPreferences: "LoginSecurity" namespace
- Firebase: Check user database structure

## Support

For issues or questions regarding the login security system:
1. Check logcat for error messages
2. Verify Firebase database connectivity
3. Test SMS API functionality
4. Review SharedPreferences data

## Important Notes

- **This system does NOT lock out users** - it only sends SMS alerts
- Users can continue attempting to login even after receiving security alerts
- The primary purpose is to notify account owners of suspicious activity
- Consider implementing additional security measures if needed (2FA, IP blocking, etc.)
