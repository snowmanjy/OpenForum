# JWT Authentication Implementation

## Overview

This module implements secure JWT-based authentication using Spring Security OAuth2 Resource Server. It follows the "Trusted Parent" pattern where JWT tokens are issued by a parent SaaS platform and verified by the OpenForum core engine.

## Architecture

### Components

1. **MemberJwtAuthenticationConverter** (`rest/auth/MemberJwtAuthenticationConverter.java`)
   - Converts validated JWT tokens into Spring Security Authentication objects
   - Implements JIT (Just-In-Time) member provisioning
   - Extracts claims: `sub` (external ID), `email`, `name`
   - Pure infrastructure concern - no framework dependencies leak into domain

2. **SecurityConfig** (`rest/config/SecurityConfig.java`)
   - Configures Spring Security OAuth2 Resource Server
   - Enables JWT signature verification
   - Registers custom authentication converter
   - Defines authorization rules

3. **Domain Integration**
   - Uses `MemberRepository` for JIT provisioning
   - Calls domain factory method `Member.create()` for new users
   - Follows Clean Architecture principles

## Security Features

### ✅ JWT Signature Verification

Unlike the previous implementation which blindly decoded JWTs, the new implementation:

- **Validates RS256/RS512 signatures** using configured public key or JWKS endpoint
- **Rejects tampered tokens** - any modification to claims fails validation
- **Enforces token expiration** (`exp` claim)
- **Validates issuer** (optional, via `iss` claim)

### ✅ JIT Provisioning

When a valid JWT arrives:

1. Extract `sub` (subject) claim as external user ID
2. Look up member by `externalId` in repository
3. If exists: Return existing member
4. If not exists: Create new member with claims (`email`, `name`) and save

This enables seamless integration with parent authentication systems (Auth0, Keycloak, custom OAuth2 servers).

### ✅ Attack Prevention

- **Signature Tampering**: Rejected by RSA signature validation
- **Token Replay**: Mitigated by expiration (`exp` claim)
- **Missing Claims**: Handled gracefully with defaults (`unknown@example.com`)
- **Invalid Format**: Rejected before reaching converter

## Configuration

### Application Configuration (application.yml)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Option 1: JWKS endpoint (recommended for production)
          jwk-set-uri: ${JWT_JWKS_URI:https://your-auth-server.com/.well-known/jwks.json}

          # Option 2: Public key file (simpler for single-key scenarios)
          public-key-location: ${JWT_PUBLIC_KEY_LOCATION:classpath:public-key.pem}

          # Optional: Validate issuer claim
          issuer-uri: ${JWT_ISSUER_URI:https://your-auth-server.com}
```

### Environment Variables

- `JWT_JWKS_URI`: URL to JWKS endpoint (e.g., Auth0, Keycloak)
- `JWT_PUBLIC_KEY_LOCATION`: Path to RSA public key in PEM format
- `JWT_ISSUER_URI`: Expected issuer value for validation

### Public Key Format

Store RSA public key in PEM format:

```
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
-----END PUBLIC KEY-----
```

Generate keys:
```bash
# Generate private key
openssl genrsa -out private-key.pem 2048

# Extract public key
openssl rsa -in private-key.pem -pubout -out public-key.pem
```

## Testing

### Unit Tests

**MemberJwtAuthenticationConverterTest**: Tests converter in isolation

- ✅ Loads existing members correctly
- ✅ Creates new members via JIT provisioning
- ✅ Handles missing claims (`email`, `name`)
- ✅ Rejects missing/blank `sub` claim
- ✅ Verifies no duplicate member creation

Run:
```bash
mvn test -pl forum-interface-rest -Dtest=MemberJwtAuthenticationConverterTest
```

### Integration Tests

**JwtAuthenticationIntegrationTest**: Full Spring Security integration

- ✅ Valid JWT with signature accepted
- ✅ Invalid signature rejected with 401
- ✅ Expired tokens rejected with 401
- ✅ Missing Authorization header rejected
- ✅ Malformed tokens rejected
- ✅ JIT provisioning creates members in database
- ✅ Existing members not duplicated

Run:
```bash
mvn test -pl forum-interface-rest -Dtest=JwtAuthenticationIntegrationTest
```

## Changes from Previous Implementation

### Before (Insecure)

```java
// ❌ Manually decoded JWT without signature verification
String[] chunks = token.split("\\.");
String payload = new String(Base64.getUrlDecoder().decode(chunks[1]));
// Any base64 string accepted as valid token!
```

**Security Risk**: Anyone could craft a JWT and gain access.

### After (Secure)

```java
// ✅ Spring Security OAuth2 validates signature
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.jwtAuthenticationConverter(memberJwtAuthenticationConverter))
)
// Only tokens signed by trusted key accepted
```

**Security Guarantee**: Only JWTs signed by the configured private key are accepted.

## JWT Claims Structure

### Required Claims

- `sub` (Subject): External user ID from parent system
- `exp` (Expiration): Unix timestamp when token expires

### Optional Claims

- `email`: User email address (defaults to `unknown@example.com`)
- `name`: User display name (defaults to `Unknown User`)
- `iss` (Issuer): Issuer identifier (validated if `issuer-uri` configured)
- `iat` (Issued At): Token issuance timestamp

### Example JWT Payload

```json
{
  "sub": "auth0|123456",
  "email": "user@example.com",
  "name": "John Doe",
  "iss": "https://your-tenant.auth0.com/",
  "iat": 1700000000,
  "exp": 1700003600
}
```

## Integration with Parent Systems

### Auth0

```yaml
spring.security.oauth2.resourceserver.jwt:
  jwk-set-uri: https://YOUR_DOMAIN.auth0.com/.well-known/jwks.json
  issuer-uri: https://YOUR_DOMAIN.auth0.com/
```

### Keycloak

```yaml
spring.security.oauth2.resourceserver.jwt:
  jwk-set-uri: https://YOUR_KEYCLOAK/realms/YOUR_REALM/protocol/openid-connect/certs
  issuer-uri: https://YOUR_KEYCLOAK/realms/YOUR_REALM
```

### Custom OAuth2 Server

Provide public key directly:

```yaml
spring.security.oauth2.resourceserver.jwt:
  public-key-location: file:/etc/openforum/keys/public-key.pem
```

## Troubleshooting

### "401 Unauthorized" despite valid token

1. Check public key matches private key used for signing:
   ```bash
   openssl rsa -pubin -in public-key.pem -text -noout
   ```

2. Verify token hasn't expired:
   ```bash
   # Decode JWT payload (without verifying signature)
   echo "JWT_PAYLOAD_PART" | base64 -d | jq .exp
   ```

3. Enable debug logging:
   ```yaml
   logging.level.org.springframework.security: DEBUG
   ```

### "No converter found for class java.time.Instant"

Ensure `spring-boot-starter-oauth2-resource-server` dependency is present.

### "Cannot access jwk-set-uri in offline mode"

Switch to `public-key-location` for environments without internet access.

## Production Checklist

- [ ] Use JWKS endpoint (not static public key) for automatic key rotation
- [ ] Configure `issuer-uri` validation
- [ ] Use HTTPS for JWKS endpoint
- [ ] Store keys in secure location (e.g., AWS Secrets Manager, HashiCorp Vault)
- [ ] Monitor failed authentication attempts
- [ ] Set appropriate token expiration times (e.g., 15 minutes)
- [ ] Implement token refresh flow in client applications

## Architecture Compliance

This implementation adheres to OpenForum's architectural principles:

✅ **Clean Architecture**: Domain (Member) has no knowledge of JWT
✅ **Dependency Inversion**: Converter depends on domain repository interface
✅ **Functional Programming**: Uses `Optional.map()`, `Optional.orElseGet()`
✅ **Immutability**: Member aggregate is immutable
✅ **Factory Pattern**: Uses `Member.create()` for domain object creation
✅ **Separation of Concerns**: Authentication in interface layer, not domain
