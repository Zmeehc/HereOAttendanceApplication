# SMS Troubleshooting Guide

This guide helps you troubleshoot SMS sending issues in the HereOAttendance app.

## Current Configuration

- **API Key**: `8a34c80dad25abdd76c4c7bcc1bf6e98`
- **API Endpoint**: `https://api.semaphore.co/api/v4/messages`
- **Sender Name**: `FarmBite`
- **SMS Provider**: Semaphore SMS API

## Quick Debug Steps

### 1. Check Logcat Output
Filter logcat by these tags to see detailed SMS logs:
```
SemaphoreSmsSender
TeacherLoginActivity
SMSTestActivity
```

### 2. Use the Debug SMS Activity
1. In TeacherLoginActivity, tap the "Debug SMS" button
2. This opens SMSTestActivity for testing SMS functionality
3. Enter your phone number and test message
4. Check the log output and logcat

### 3. Test SMS Manually
```java
// Test with your phone number
SemaphoreSmsSender.testSMS("+639663996287");
```

## Common Issues and Solutions

### Issue 1: No SMS Received
**Symptoms**: No SMS delivered, no error messages in logs

**Possible Causes**:
- API key expired or invalid
- Insufficient SMS credits
- Phone number format incorrect
- Network connectivity issues

**Solutions**:
1. Verify API key is valid at [Semaphore Dashboard](https://semaphore.co/dashboard)
2. Check SMS credits balance
3. Ensure phone number starts with `+63` for Philippines
4. Test network connectivity

### Issue 2: API Key Invalid
**Symptoms**: HTTP 401 or 403 response codes

**Solutions**:
1. Generate new API key from Semaphore dashboard
2. Update `API_KEY` constant in `SemaphoreSmsSender.java`
3. Ensure API key has SMS sending permissions

### Issue 3: Phone Number Format
**Symptoms**: SMS not delivered to correct number

**Solutions**:
- Philippines numbers should start with `+63`
- Format: `+639XXXXXXXXX`
- Remove leading zeros from mobile numbers

### Issue 4: Network Errors
**Symptoms**: Connection timeout or network errors

**Solutions**:
1. Check internet connection
2. Verify firewall settings
3. Test with different network (WiFi vs Mobile data)

## Debug Commands

### Check API Configuration
```java
String config = SemaphoreSmsSender.getApiConfig();
Log.i("SMS", config);
```

### Test SMS Sending
```java
SemaphoreSmsSender.testSMS("+639663996287");
```

### Manual SMS Send
```java
SemaphoreSmsSender.sendSMS("+639663996287", "Test message");
```

## Logcat Analysis

### Successful SMS
```
I/SemaphoreSmsSender: Starting SMS send process to: +639663996287
I/SemaphoreSmsSender: Message: Test message
I/SemaphoreSmsSender: Formatted phone number: +639663996287
I/SemaphoreSmsSender: HTTP Response Code: 200
I/SemaphoreSmsSender: SMS sent successfully! Response: {...}
```

### Failed SMS
```
E/SemaphoreSmsSender: Failed to send SMS. Response code: 401
E/SemaphoreSmsSender: Error response: {"error": "Invalid API key"}
```

### Network Errors
```
E/SemaphoreSmsSender: Error sending SMS
E/SemaphoreSmsSender: Error details: Connection timeout
```

## Testing Checklist

- [ ] API key is valid and active
- [ ] Sufficient SMS credits
- [ ] Phone number format correct (+63XXXXXXXXX)
- [ ] Internet connection working
- [ ] No firewall blocking HTTPS requests
- [ ] App has INTERNET permission
- [ ] Test with debug SMS activity

## Alternative SMS Providers

If Semaphore continues to have issues, consider:

1. **Twilio** - More reliable, better documentation
2. **MessageBird** - Good international coverage
3. **Nexmo/Vonage** - Enterprise-grade reliability

## Contact Support

1. **Semaphore Support**: [support@semaphore.co](mailto:support@semaphore.co)
2. **Check API Status**: [status.semaphore.co](https://status.semaphore.co)
3. **API Documentation**: [docs.semaphore.co](https://docs.semaphore.co)

## Production Considerations

- Remove debug button before production release
- Implement proper error handling and user feedback
- Add SMS delivery confirmation
- Consider rate limiting for SMS alerts
- Monitor SMS costs and usage

## Quick Fix Commands

```bash
# Check if app can reach Semaphore API
curl -X POST "https://api.semaphore.co/api/v4/messages" \
  -H "Content-Type: application/json" \
  -d '{"apikey":"8a34c80dad25abdd76c4c7bcc1bf6e98","number":"+639663996287","message":"Test","sendername":"FarmBite"}'
```

## Next Steps

1. **Immediate**: Use debug SMS activity to test
2. **Short-term**: Check API key validity and credits
3. **Long-term**: Consider alternative SMS provider if issues persist
