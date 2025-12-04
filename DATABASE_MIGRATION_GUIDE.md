# Database Migration Guide for Production

## 🚨 Current Problem

**Your production database is missing tables!**

Error:
```
ERROR: relation "invoice" does not exist
```

**Root Cause:**
- `spring.jpa.hibernate.ddl-auto=create-drop` in application.properties
- This setting **DELETES ALL DATA** when the app stops
- Production database is empty after each restart

## ✅ Immediate Fix (Quick)

### Set Environment Variable in Digital Ocean

1. Go to Digital Ocean Console → Your App
2. Settings → Environment Variables
3. Add new variable:
   - **Name:** `SPRING_JPA_HIBERNATE_DDL_AUTO`
   - **Value:** `update`
4. Click "Save"
5. Redeploy the app

**This will:**
- ✅ Create tables on first startup
- ✅ Update schema when entities change
- ✅ Keep your data safe (never drops tables)

---

## 🎯 Better Solution: Use Flyway (Recommended for Production)

### Why Flyway?

- ✅ Version-controlled database migrations
- ✅ Safe, repeatable deployments
- ✅ Rollback support
- ✅ Team collaboration
- ✅ Audit trail of all schema changes

### Step 1: Add Flyway Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### Step 2: Create Migration Directory

```bash
mkdir -p src/main/resources/db/migration
```

### Step 3: Create Initial Migration

Create file: `src/main/resources/db/migration/V1__initial_schema.sql`

```sql
-- V1__initial_schema.sql
-- Initial database schema for invoice-service-api-v3

CREATE TABLE IF NOT EXISTS invoice (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    subscription_id VARCHAR(255),  -- Nullable for one-time payments
    invoice_id_provided_by_stripe VARCHAR(255),
    last_payment_intent_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    items JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_invoice_user_id ON invoice(user_id);
CREATE INDEX IF NOT EXISTS idx_invoice_customer_id ON invoice(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoice_subscription_id ON invoice(subscription_id);
CREATE INDEX IF NOT EXISTS idx_invoice_status ON invoice(status);
CREATE INDEX IF NOT EXISTS idx_invoice_created_at ON invoice(created_at DESC);

-- Add any other tables here
```

### Step 4: Configure Application

Update `application.properties`:

```properties
# CHANGE THIS:
# spring.jpa.hibernate.ddl-auto=create-drop

# TO THIS (for Flyway):
spring.jpa.hibernate.ddl-auto=validate

# Flyway configuration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

### Step 5: Future Migrations

For each schema change, create a new migration:

**Example: `V2__add_payment_method_column.sql`**
```sql
ALTER TABLE invoice 
ADD COLUMN payment_method VARCHAR(255);

CREATE INDEX idx_invoice_payment_method ON invoice(payment_method);
```

**Naming Convention:**
- `V{version}__{description}.sql`
- `V1__initial_schema.sql`
- `V2__add_payment_method.sql`
- `V3__create_customer_table.sql`

### Step 6: Deploy

1. Commit migrations to git
2. Push to repository
3. Digital Ocean will:
   - Run Flyway migrations automatically
   - Create/update tables as needed
   - Track which migrations have been applied

---

## 🔧 Manual Database Fix (Emergency)

If you need to fix the database **right now** without code changes:

### Connect to Production Database

```bash
# Get database credentials from Digital Ocean
# Then connect with psql:
psql postgresql://username:password@host:port/database
```

### Run DDL Manually

```sql
-- Create invoice table
CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    subscription_id VARCHAR(255),
    invoice_id_provided_by_stripe VARCHAR(255),
    last_payment_intent_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    items JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_invoice_user_id ON invoice(user_id);
CREATE INDEX idx_invoice_customer_id ON invoice(customer_id);
CREATE INDEX idx_invoice_subscription_id ON invoice(subscription_id);
CREATE INDEX idx_invoice_status ON invoice(status);
CREATE INDEX idx_invoice_created_at ON invoice(created_at DESC);
```

---

## 📊 Comparison

| Method | Pros | Cons | Use Case |
|--------|------|------|----------|
| **`ddl-auto=create-drop`** | ❌ NEVER USE IN PRODUCTION | Deletes all data! | Local dev only |
| **`ddl-auto=update`** | ✅ Simple, automatic | Limited control, no rollbacks | Quick fix, small projects |
| **Flyway** | ✅ Professional, version-controlled, safe | More setup | Production systems |
| **Manual SQL** | ✅ Immediate fix | No automation, error-prone | Emergency only |

---

## 🎯 Recommended Approach

### For Now (Immediate):
1. Set `SPRING_JPA_HIBERNATE_DDL_AUTO=update` in Digital Ocean
2. Redeploy
3. Test the payment endpoint

### For Long-term (This Week):
1. Implement Flyway migrations
2. Change `ddl-auto` to `validate`
3. Create `V1__initial_schema.sql` from your current entities
4. Deploy and test

---

## ✅ Verification

After applying the fix:

```bash
# Check if tables exist
psql $DATABASE_URL -c "\dt"

# Should see:
#  Schema |   Name   | Type  |  Owner
# --------+----------+-------+----------
#  public | invoice  | table | postgres

# Test the endpoint
curl -X POST https://invoice-service-api-v3-r98x6.ondigitalocean.app/api/v1/invoice/create-payment \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "userId": "test-user",
    "email": "test@example.com",
    "payment_method": "pm_test",
    "currency": "usd",
    "items": [{"price": "price_test"}]
  }'

# Should get 200 OK (not 500)
```

---

## 📚 Additional Resources

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Database Initialization](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization)
- [Hibernate DDL Auto Settings](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.data.spring.jpa.hibernate.ddl-auto)

---

**TL;DR:**
1. Go to Digital Ocean Console
2. Add environment variable: `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
3. Redeploy
4. Test payment endpoint
5. (Later) Implement Flyway for proper migrations

