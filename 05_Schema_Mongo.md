## Collection: user_preferences
### JSON Schema Validation

```json
{
  "bsonType": "object",
  "required": ["userId", "language", "timezone", "notifications"],
  "properties": {
    "_id": {
      "bsonType": "objectId"
    },
    "userId": {
      "bsonType": "objectId",
      "description": "Reference to users._id",
      "uniqueItems": true
    },
    "language": {
      "bsonType": "string",
      "description": "Locale code, e.g. 'en-US', 'es-AR'"
    },
    "timezone": {
      "bsonType": "string",
      "description": "IANA time zone, e.g. 'America/Argentina/Buenos_Aires'"
    },
    "defaultCurrency": {
      "bsonType": "string",
      "description": "Default display currency, e.g. 'USD', 'EUR'",
      "default": "USD"
    },
    "notifications": {
      "bsonType": "object",
      "required": ["email", "sms", "push", "inApp"],
      "properties": {
        "email": { "bsonType": "bool" },
        "sms":   { "bsonType": "bool" },
        "push":  { "bsonType": "bool" },
        "inApp": { "bsonType": "bool" }
      }
    },
    "preferences": {
      "bsonType": "object",
      "description": "Additional custom settings",
      "properties": {
        "dailyReport":        { "bsonType": "bool",  "default": false },
        "fraudAlerts":        { "bsonType": "bool",  "default": true  },
        "maxTransactionAmt":  { "bsonType": "double", "minimum": 0    }
      }
    },
    "createdAt": {
      "bsonType": "date",
      "description": "When this doc was first created"
    },
    "updatedAt": {
      "bsonType": "date",
      "description": "Last time preferences were modified"
    }
  }
}
```

### Indexes
```js
db.user_preferences.createIndex(
  { userId: 1 },
  { unique: true, name: "ux_user_preferences_userId" }
);
db.user_preferences.createIndex(
  { updatedAt: 1 },
  { expireAfterSeconds: 60 * 60 * 24 * 365, name: "idx_prefs_ttl" }
);

```

- Unique on userId ensures one prefs doc per user.
- TTL index on updatedAt can optionally purge stale docs after a year.
