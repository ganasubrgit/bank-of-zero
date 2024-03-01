# Frontend Service

The frontend service manages the user-facing web interface for the application.

Implemented in Python with Flask.

### Endpoints

| Endpoint   | Type  | Auth? | Description                                                                               |
| ---------- | ----- | ----- | ----------------------------------------------------------------------------------------- |
| `/`        | GET   | 🔒    |  Renders `/home` or `/login` based on authentication status. Must always return 200       |
| `/deposit` | POST  | 🔒    |  Submits a new external deposit transaction to `ledgerwriter`                             |
| `/home`    | GET   | 🔒    |  Renders homepage if authenticated Otherwise redirects to `/login`                        |
| `/login`   | GET   |       |  Renders login page if not authenticated. Otherwise redirects to `/home`                  |
| `/login`   | POST  |       |  Submits login request to `userservice`                                                   |
| `/logout`  | POST  | 🔒    | delete local authentication token and redirect to `/login`                                |
| `/payment` | POST  | 🔒    |  Submits a new internal payment transaction to `ledgerwriter`                             |
| `/signup`  | GET   |       |  Renders signup page if not authenticated. Otherwise redirects to `/home`                 |
| `/signup`  | POST  |       |  Submits new user signup request to `userservice`                                         |
