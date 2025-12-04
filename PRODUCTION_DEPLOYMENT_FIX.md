# 🚨 IMMEDIATE FIX - Production Database Error

## The Problem

```
ERROR: relation "invoice" does not exist
```

**Why:** Your production database has no tables because `ddl-auto=create-drop` deletes everything on restart.

---

## ✅ QUICK FIX (5 minutes)

### Step-by-Step:

#### 1. Go to Digital Ocean Console
```
https://cloud.digitalocean.com/apps/
```

#### 2. Select Your App
- Click on `invoice-service-api-v3-r98x6`

#### 3. Go to Settings
- Click "Settings" tab
- Click "App-Level Environment Variables"

#### 4. Add Environment Variable
Click "Edit" → "Add Variable":

```
Name:  SPRING_JPA_HIBERNATE_DDL_AUTO
Value: update
Type:  Environment Variable (not secret)
Scope: All components
```

#### 5. Save & Redeploy
- Click "Save"
- Digital Ocean will automatically redeploy
- Wait 2-3 minutes for deployment to complete

#### 6. Test
```bash
# Your payment endpoint should now work:
curl -X POST https://invoice-service-api-v3-r98x6.ondigitalocean.app/api/v1/invoice/create-payment \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "userId": "7edc3335-671b-44f1-99df-c856fea3f533",
    "email": "test@example.com",
    "payment_method": "pm_test",
    "currency": "usd",
    "items": [{"price": "price_1LkVWUE4VxS976BcvpZMKUs3"}]
  }'
```

**Expected Response:** `200 OK` (not `500`)

---

## 🎯 What This Does

### `ddl-auto=update` means:
- ✅ Creates tables on **first startup** (fixes your current issue)
- ✅ Updates schema when entities change
- ✅ **Keeps all data** (never drops tables)
- ✅ Safe for production

### Before (create-drop):
```
App starts  → Tables created
App stops   → ❌ ALL DATA DELETED
App restarts → Empty database
Request     → ❌ ERROR: table doesn't exist
```

### After (update):
```
App starts  → Tables created (if missing)
App stops   → ✅ Data preserved
App restarts → ✅ Data still there
Request     → ✅ SUCCESS
```

---

## 📸 Screenshot Guide

### Where to Add Environment Variable:

```
Digital Ocean Console
└── Apps
    └── invoice-service-api-v3-r98x6
        └── Settings (tab)
            └── App-Level Environment Variables
                └── Edit
                    └── Add Variable
                        Name:  SPRING_JPA_HIBERNATE_DDL_AUTO
                        Value: update
                        └── Save
```

---

## ⚠️ Important Notes

### This is a Quick Fix
- ✅ Solves immediate problem
- ✅ Safe for production
- ⚠️ Not the "best practice" for large teams

### For Long-Term (recommended)
- Use **Flyway** or **Liquibase** for database migrations
- See `DATABASE_MIGRATION_GUIDE.md` for details

---

## 🔍 Verify Environment Variable Was Set

After saving, you should see:

```
Environment Variables:
- SPRING_JPA_HIBERNATE_DDL_AUTO = update  ✅
- (other variables...)
```

---

## 🐛 Troubleshooting

### Still getting the error after redeploy?

**Check deployment logs:**
1. Go to "Activity" tab in Digital Ocean
2. Click on latest deployment
3. Look for:
   ```
   Hibernate: create table invoice (...)  ✅ Good!
   ```

### If tables still not created:

**Option A: Restart the app**
1. Go to "Runtime" tab
2. Click "Force Rebuild and Deploy"

**Option B: Check database connection**
1. Verify `DATABASE_URL` is set correctly
2. Check database exists in Digital Ocean

**Option C: Manual table creation** (Emergency only)
1. Connect to database via Digital Ocean console
2. Run SQL from `DATABASE_MIGRATION_GUIDE.md`

---

## ✅ Success Indicators

### Deployment Logs (Good):
```
Creating table invoice...
Creating indexes...
Application started successfully
```

### API Response (Good):
```json
{
  "status": "succeeded",
  "message": "One-time payment processed successfully.",
  "data": {
    "id": "...",
    "userId": "...",
    "status": "succeeded"
  }
}
```

### API Response (Bad):
```json
{
  "status": "error",
  "message": "ERROR: relation \"invoice\" does not exist"
}
```

---

## 🎉 After the Fix

Once working:
1. ✅ Test payment endpoint
2. ✅ Test subscription endpoint
3. ✅ Verify database persists data across restarts
4. 📚 Read `DATABASE_MIGRATION_GUIDE.md` for proper long-term solution

---

**TLDR:**
1. Digital Ocean Console → Your App → Settings
2. Add env var: `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
3. Save & wait for redeploy
4. Test payment endpoint
5. ✅ Done!

