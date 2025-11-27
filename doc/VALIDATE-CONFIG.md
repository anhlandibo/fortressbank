# Kong Configuration Validation

## ⚠️ IMPORTANT: Always validate Kong config before committing!

### Method 1: Using Kong Docker Container (PREFERRED)
```bash
# Start Docker Desktop first, then run:
docker exec fortressbank-kong-1 kong config parse /usr/local/kong/declarative/kong.yml
```

**Expected output on success:**
```
parse successful
```

### Method 2: Using Python Validator (Offline)
```bash
python validate-kong-config.py kong/kong.yml
```

This validates:
- ✅ YAML syntax
- ✅ Required fields (services, routes, plugins)
- ✅ Plugin configurations
- ✅ Known plugin names
- ✅ Rate limiting config
- ✅ OpenID Connect config

### Method 3: Manual Validation Checklist

**Format Version:**
- [ ] `_format_version: "3.0"` present

**Services:**
- [ ] Each service has `name` and `url`
- [ ] Each route has `paths`

**Plugins:**
- [ ] Single `plugins:` array (no duplicates)
- [ ] Each plugin has `name` and `config`
- [ ] Service-scoped plugins use `service: <name>`
- [ ] No duplicate plugin instances on same service

**Rate Limiting:**
- [ ] At least one time limit (second/minute/hour)
- [ ] Valid policy: local/cluster/redis

**OpenID Connect:**
- [ ] Has client_id, client_secret, discovery

## 🔒 Critical Files Checklist
Before committing configuration changes:
1. ✅ Validate Kong config (`kong/kong.yml`)
2. ✅ Check Docker Compose files (no syntax errors)
3. ✅ Verify Keycloak realm config (if changed)
4. ✅ Test in local environment first
