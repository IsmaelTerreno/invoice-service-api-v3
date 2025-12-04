# 🦅 Flyway Deployment Guide - Invoice Service API v3

## ✅ What We've Implemented

Flyway database migration system is now fully configured! Here's what changed:

### 1. **Dependencies Added** (`pom.xml`)
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

### 2. **Migration Created** (`src/main/resources/db/migration/V1__initial_schema.sql`)
- ✅ Creates `invoice` table with all columns
- ✅ Adds indexes for performance (user_id, customer_id, status, etc.)
- ✅ Includes column comments for documentation

### 3. **Configuration Updated**
- **`application.properties`**: Flyway enabled, `ddl-auto=validate`
- **`application-prod.properties`**: Production-optimized settings
- **`application-local.properties`**: Local development with Flyway

---

## 🚀 Deployment to Digital Ocean (Production)

### Option A: Quick Deploy (No Environment Changes Needed)

Since we configured Flyway in `application.properties`, you can deploy immediately:

```bash
# 1. Commit and push changes
git add .
git commit -m "Add Flyway database migrations"
git push origin main

# 2. Digital Ocean will automatically:
#    - Pull latest code
#    - Build with Flyway dependencies
#    - Run V1 migration on startup
#    - Create invoice table
#    - Your API will work! ✅
```

**What happens on first deployment:**
```
App starts
↓
Flyway detects database is empty (or has tables from old ddl-auto)
↓
Flyway creates flyway_schema_history table
↓
Flyway runs V1__initial_schema.sql
↓
invoice table created with indexes
↓
V1 recorded in flyway_schema_history
↓
App ready to accept requests ✅
```

### Option B: Use Production Profile (Recommended)

Set this environment variable in Digital Ocean for extra safety:

```
SPRING_PROFILES_ACTIVE=prod
```

This activates `application-prod.properties` with production-optimized settings.

**Steps:**
1. Digital Ocean Console → Your App → Settings
2. App-Level Environment Variables → Edit
3. Add: `SPRING_PROFILES_ACTIVE=prod`
4. Save and redeploy

---

## 🧪 Testing Locally (Before Production)

### Prerequisites
- PostgreSQL running on `localhost:5433`
- Database `remotejob` exists
- Credentials: `postgres/postgres`

### Test Steps

#### 1. **Clean Your Local Database** (Optional but recommended)
```sql
-- Connect to your local database
psql -h localhost -p 5433 -U postgres -d remotejob

-- Drop invoice table if it exists (from old ddl-auto)
DROP TABLE IF EXISTS invoice CASCADE;

-- Check flyway_schema_history
SELECT * FROM flyway_schema_history;
-- (If table doesn't exist, that's normal for first run)

-- Exit
\q
```

#### 2. **Build the Project**
```bash
cd /Users/ismaelterreno/Documents/Repository/invoice-service-api-v3

# Clean and compile
./mvnw clean compile

# Or build package
./mvnw clean package -DskipTests
```

#### 3. **Run the Application**
```bash
# Option 1: Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Option 2: JAR
java -jar target/invoice-service-api-v3-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

#### 4. **Watch the Logs**

You should see Flyway messages like:

```
✅ GOOD LOGS:
INFO o.f.c.i.license.VersionPrinter      : Flyway Community Edition 9.x.x by Redgate
INFO o.f.c.i.database.base.BaseDatabaseType : Database: jdbc:postgresql://localhost:5433/remotejob (PostgreSQL 15.x)
INFO o.f.core.internal.command.DbValidate : Successfully validated 1 migration (execution time 00:00.012s)
INFO o.f.c.i.s.JdbcTableSchemaHistory    : Creating Schema History table "public"."flyway_schema_history" ...
INFO o.f.core.internal.command.DbMigrate  : Current version of schema "public": << Empty Schema >>
INFO o.f.core.internal.command.DbMigrate  : Migrating schema "public" to version "1 - initial schema"
INFO o.f.core.internal.command.DbMigrate  : Successfully applied 1 migration to schema "public", now at version v1 (execution time 00:00.145s)
```

```
❌ ERROR LOGS (if you see these):

ERROR o.f.core.internal.command.DbMigrate : Migration V1__initial_schema.sql failed
SQL State  : 42P07
Error Code : 0
Message    : ERROR: relation "invoice" already exists

FIX: Drop the existing table and restart:
DROP TABLE invoice CASCADE;
```

#### 5. **Verify Database**

```bash
psql -h localhost -p 5433 -U postgres -d remotejob
```

```sql
-- Check Flyway history
SELECT installed_rank, version, description, type, success, installed_on 
FROM flyway_schema_history;

-- Expected output:
-- installed_rank | version | description    | type | success | installed_on
-- ---------------+---------+----------------+------+---------+-------------
-- 1              | 1       | initial schema | SQL  | true    | 2025-12-04 ...

-- Check invoice table exists
\dt invoice

-- Check table structure
\d invoice

-- Check indexes
\di invoice*

-- Should see:
-- idx_invoice_user_id
-- idx_invoice_customer_id
-- idx_invoice_status
-- etc.
```

#### 6. **Test the API**

```bash
# Health check
curl http://localhost:3120/actuator/health

# Create a payment (should work now!)
curl -X POST http://localhost:3120/api/v1/invoice/create-payment \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_LOCAL_JWT" \
  -d '{
    "userId": "test-user-123",
    "email": "test@example.com",
    "payment_method": "pm_card_visa",
    "currency": "usd",
    "items": [{"price": "price_test"}]
  }'

# Expected: 200 OK (not 500!)
```

---

## 📊 Understanding Flyway Tables

### `flyway_schema_history` Table

This table tracks all migrations:

```sql
SELECT * FROM flyway_schema_history;
```

| Column | Description |
|--------|-------------|
| `installed_rank` | Order of execution (1, 2, 3...) |
| `version` | Migration version (from filename V1, V2, V3...) |
| `description` | Description (from filename after `__`) |
| `type` | SQL, JDBC, or BASELINE |
| `script` | Filename of the migration |
| `checksum` | Hash to detect changes |
| `installed_by` | Database user who ran it |
| `installed_on` | Timestamp of execution |
| `execution_time` | How long it took (milliseconds) |
| `success` | TRUE if successful, FALSE if failed |

**This table is sacred!** 
- ❌ Don't manually edit it
- ❌ Don't delete rows
- ✅ Use it for debugging
- ✅ Use it for audit trails

---

## 🔄 Adding Future Migrations

When you need to change the database schema:

### Step 1: Create New Migration File

```bash
# Example: Adding a payment_method column
touch src/main/resources/db/migration/V2__add_payment_method_column.sql
```

### Step 2: Write the SQL

```sql
-- V2__add_payment_method_column.sql
ALTER TABLE invoice 
ADD COLUMN payment_method VARCHAR(255);

CREATE INDEX idx_invoice_payment_method ON invoice(payment_method);

COMMENT ON COLUMN invoice.payment_method IS 'Payment method used (e.g., card, bank_transfer)';
```

### Step 3: Update Your Entity (Optional)

```java
@Entity
public class Invoice {
    // ... existing fields ...
    
    @Column(name = "payment_method")
    private String paymentMethod;
}
```

### Step 4: Deploy

```bash
git add src/main/resources/db/migration/V2__add_payment_method_column.sql
git commit -m "Add payment_method column to invoice"
git push origin main
```

**What happens:**
- Local: Restart app → Flyway runs V2 → Column added
- Production: Deploy → Flyway runs V2 → Column added
- **No data loss!** All existing invoices kept, new column is NULL

---

## 🛡️ Safety Features

### 1. **Validation**

Flyway validates migrations on every startup:

```
✅ All applied migrations still exist in code
✅ No applied migrations have been modified (checksum match)
✅ Migrations are in correct sequence
```

If validation fails, app won't start (preventing corruption).

### 2. **Checksum Protection**

If you try to change an already-applied migration:

```
❌ ERROR: Validate failed: Migration checksum mismatch for migration version 1
   Applied to database : 1234567890
   Resolved locally    : 9876543210
```

**Fix:** Don't change old migrations! Create a new one instead.

### 3. **Baseline on Migrate**

`spring.flyway.baseline-on-migrate=true` means:
- If database already has tables (from old `ddl-auto`), Flyway won't try to recreate them
- It records V1 as "baseline" and continues from there
- Safe for migrating from `ddl-auto` to Flyway

### 4. **Transaction Rollback**

Each migration runs in a transaction:

```sql
BEGIN;
  CREATE TABLE invoice (...);
  CREATE INDEX idx_1 ON invoice(user_id);
  CREATE INDEX idx_2 ON invoice(customer_id); -- FAILS
ROLLBACK; -- Nothing is created
```

---

## 🐛 Troubleshooting

### Problem: "Table already exists"

**Cause:** Database has tables from old `ddl-auto` setup.

**Solution 1:** Baseline
```properties
spring.flyway.baseline-on-migrate=true  # Already set!
```

**Solution 2:** Clean database (LOCAL ONLY!)
```sql
DROP TABLE invoice CASCADE;
DROP TABLE flyway_schema_history CASCADE;
```

### Problem: "Checksum mismatch"

**Cause:** You modified a migration that was already applied.

**Solution:** Don't change old migrations! Create a new one:
```bash
# Don't edit V1__initial_schema.sql
# Instead create:
touch src/main/resources/db/migration/V2__fix_issue.sql
```

### Problem: "Failed migration"

**Cause:** SQL error in migration.

**Check flyway_schema_history:**
```sql
SELECT * FROM flyway_schema_history WHERE success = FALSE;
```

**Fix:**
1. Manually fix the issue in database
2. Delete failed migration from `flyway_schema_history`
3. Fix the SQL in migration file
4. Restart app

### Problem: "Flyway not running"

**Check:**
```properties
spring.flyway.enabled=true  # Must be true
```

**Check logs:**
```
Should see: "Flyway Community Edition ... by Redgate"
```

---

## 📋 Deployment Checklist

### Pre-Deployment

- [ ] All migration files are in `src/main/resources/db/migration/`
- [ ] Migration files follow naming: `V{number}__{description}.sql`
- [ ] SQL is tested locally
- [ ] `flyway_schema_history` shows successful migration locally
- [ ] Application starts without errors locally
- [ ] API endpoints work with new schema

### Deployment

- [ ] Code committed and pushed to Git
- [ ] Digital Ocean triggered deployment (or manual trigger)
- [ ] Check deployment logs for Flyway messages
- [ ] Verify `flyway_schema_history` in production database
- [ ] Test API endpoints in production
- [ ] Monitor for errors

### Post-Deployment

- [ ] Create a test payment to verify database works
- [ ] Check CloudFlare/load balancer health checks
- [ ] Monitor application logs for issues
- [ ] Update team on migration success

---

## 🎯 Quick Reference

### Local Testing
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Check Flyway Status
```sql
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank;
```

### Create New Migration
```bash
touch src/main/resources/db/migration/V{next_number}__{description}.sql
```

### Production Deploy
```bash
git add .
git commit -m "Add migration VX__description"
git push origin main
```

---

## 📚 Additional Resources

- [Flyway Official Docs](https://flywaydb.org/documentation/)
- [Spring Boot Flyway Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)
- [PostgreSQL Data Types](https://www.postgresql.org/docs/current/datatype.html)

---

## ✅ Summary

**What Changed:**
- ✅ Added Flyway dependencies
- ✅ Created V1 initial schema migration
- ✅ Configured Flyway in application properties
- ✅ Changed from `ddl-auto=create-drop` to `ddl-auto=validate`

**Benefits:**
- ✅ No more data loss on restart
- ✅ Version-controlled database changes
- ✅ Safe production deployments
- ✅ Team collaboration on schema changes
- ✅ Audit trail of all migrations

**Next Steps:**
1. Test locally (optional but recommended)
2. Commit and push to Git
3. Deploy to Digital Ocean
4. Verify production database
5. Test payment endpoint ✅

**Your production error will be fixed!** 🎉

The `ERROR: relation "invoice" does not exist` error will disappear because Flyway will create the table on first startup.

