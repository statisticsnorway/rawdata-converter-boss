# Boss 😎

## Resource tree example:
```
job
├── available
│   ├── altinn3
│   │   ├── b7eccadd-d5ec-4f69-9ca7-c1c3b75d278f
│   │   └── ff153d9c-3a6c-46bd-b170-18f72a061e95
│   ├── freg
│   └── sirius
│       └── f0c6049c-08d9-43ca-8bb3-41e596f39d56
├── active
│   ├── altinn3
│   ├── freg
│   └── sirius
│       └── a3db30f7-3b77-47af-931f-5d7c69da71df
└── done
    ├── altinn3
    │   └── f5bc84e8-e36f-47dd-804f-021edc5a1f21
    ├── freg
    │   ├── 99f681a5-c24b-4b76-9384-0a72957c5482
    │   └── e6c030e7-24fd-46be-a803-11a22a22adbb
    └── sirius
        ├── a04750ac-f8dc-410c-ae54-cfb81cbe5661
        └── a3214ea2-53f8-41eb-bc9d-6b654446d45a

```

## Endpoints:

### Get an available job from any source
```
GET /job/available

HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "e80b99fd-1dc6-40e9-9719-77b0fbab768b",
  "source": "altinn3",
  "job": {
    "storageRoot": "gs://bucket",
    "storagePath": "/tmp",
    "storageVersion": 42,
    "topic": "data",
    "initialPosition": "LAST"
  }
}
```

### Get an available job from a specific source
```
GET /job/available/{source}

HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "e80b99fd-1dc6-40e9-9719-77b0fbab768b",
  "source": "altinn3",
  "job": {
    "storageRoot": "gs://bucket",
    "storagePath": "/tmp",
    "storageVersion": 42,
    "topic": "data",
    "initialPosition": "LAST"
  }
}
```

### Get a specific job
```
GET /job/available/{source}/{id}

HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "e80b99fd-1dc6-40e9-9719-77b0fbab768b",
  "source": "altinn3",
  "job": {
    "storageRoot": "gs://bucket",
    "storagePath": "/tmp",
    "storageVersion": 42,
    "topic": "data",
    "initialPosition": "LAST"
  }
}
```

### Check on an active job
```
HEAD /job/active/{source}/{id}

HTTP/1.1 200 OK
```

## Converter interactions with this API
1. Poll for an available job:
    `GET /job/available` or `GET /job/available/{source}`
2. When doing a job, routinely check if it's still active:
    `HEAD /job/active/{source}/{id}`
3. If job isn't active anymore, stop and do step 1 again
4. When job is done, notify boss and start at step 1 again:
    `POST /job/done/{source}/{id}`

## Administrative interactions with this API
1. Submit a job:
    `POST /job/available/{source}` (an id will be generated) or `POST /job/available/{source}/{id}`
2. Stop a job: (not immediate):
    `POST /job/done/{source}/{id}`

## Possible job transitions
* available => active `GET /job/available`
* available => done   `POST /job/done/{source}/{id}` (will only work if there's an available or active job with that id) 
* active => done      `POST /job/done/{source}/{id}` (will only work if there's an available or active job with that id)

## Database schema
```sql
CREATE TABLE job
(
    id       varchar(26) PRIMARY KEY,
    status   varchar(100),
    source   varchar(100),
    document jsonb
);
```

### Queries
* Get an available job from any source
    ```sql
    UPDATE job
    SET status = 'ACTIVE'
    WHERE id =
        (SELECT id FROM job WHERE status = 'AVAILABLE' ORDER BY id LIMIT 1)
    RETURNING *
    ```
