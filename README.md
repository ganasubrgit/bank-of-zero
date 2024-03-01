# Bank of Zero (demo app)

A demo bank application to demonstrate SRE and Observability on a hybrid N-Tier architecture comprising of a Frontend/Web Server, App Server [Backend], Database and Messaging Middleware. 

## Architecture

![](/image/image.jpeg)

## Tech Stack

**Frontend:** 
Python Flask Framework [Rest API]

**Web Server:** 
WSGI

**App Server:** 
* Account Management Service - Java SpringBoot 3 [Rest API]
* Deposit Service - Java SpringBoot 3 [Hybrid]

**Messaging Middleware:** Apache Artemis MQ

**Database:** 
* User Account Data - Mongo DB Cloud
* Transactional Data - MySql



## Observability Tools

**Metrics:**
* Open Telemetry [OTLP]
* Spring Boot Actuators [Prometheus Exposition Format]
* Prometheus Exporters [Prometheus Exposition Format]

**Logs:**
* Loki

**Traces:**
* Jaeger [OTLP]

**Visualization:**
* Grafana

**Agents:**
* Java Open Telemetry Agent
* MySqld Prometheus Exporter
* Mongo Prometheus Exporter

**Observability Gateway:**
* Open Telemetry Collector

## API Reference

#### Frontend

| Endpoint   | Type  | Auth? | Description                                                                               |
| ---------- | ----- | ----- | ----------------------------------------------------------------------------------------- |
| `/`        | GET   | 🔒    |  Renders `/home` or `/login` based on authentication status. Must always return 200       |
| `/deposit` | POST  | 🔒    |  Submits a new external deposit transaction to `deposit-service`                             |
| `/home`    | GET   | 🔒    |  Renders homepage if authenticated Otherwise redirects to `/login`                        |
| `/login`   | GET   |       |  Renders login page if not authenticated. Otherwise redirects to `/home`                  |
| `/login`   | POST  |       |  Submits login request to `account-management` service                                                   |
| `/logout`  | POST  | 🔒    | delete local authentication token and redirect to `/login`                                |
| `/payment` | POST  | 🔒    |  Submits a new internal payment transaction to `deposit-service`                             |
| `/signup`  | GET   |       |  Renders signup page if not authenticated. Otherwise redirects to `/home`                 |
| `/signup`  | POST  |       |  Submits new user signup request to `account-management` service                                         |

#### Account Management Service

| Endpoint   | Type  | Auth? | Description                                                                               |
| ---------- | ----- | ----- | ----------------------------------------------------------------------------------------- |
| `/actuator/prometheus`        | GET   |     |  Returns a web end point with prometheus metrics in exposition format    |
| `/api/auth/signup` | POST  |    |  Gets user details as payload and creates a record in Mongo DB                             |
| `/api/auth/signin`    | POST   | 🔒    |  Gets username and password as payload  for authentication with Mongo DB and returns a secure jwt token and cookie                        |


#### Deposit Service

| Endpoint   | Type  | Auth? | Description                                                                               |
| ---------- | ----- | ----- | ----------------------------------------------------------------------------------------- |
| `/actuator/prometheus`        | GET   |     |  Returns a web end point with prometheus metrics in exposition format    |
| `/account/create` | POST  |    |  Creates an account in MySQL Database to sync the data with MongoDB                             |
| `/account/update`    | PUT   |     |  Updates user data in MySQL                      |
| `/account/get-all` | GET  |    |  Returns all the user data from MySQL database           |
| `/account/exists`    | GET   |     |  Validates if a given user is already existing in MySQL Database and sends a boolean response                      |
| `/account/fetch-balance` | GET  |    |  Fetches the account balance of user given as parameter                            |
| `/account/get-transactions`    | GET   |     |  Gets all the transactions data [credit/debit] from MYSQL Database                      |
| `/account/get-transactions-by-name` | GET  |    |  Returns all the user data from MySQL database           |
| `/account/transfer`    | POST   |     |  Transfers amount to reciepient and updates MySQL Database                      |
| `/account/deposit` | POST  |    |  Deposits amount to account and updates MySQL Database                            |
                     

### Account Management API's

```
  POST /api/auth/signup
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `name` | `string` | **Required**. Account Name/User Name |
| `email` | `string` | **Required**. Email ID |
| `password` | `string` | **Required**. User Password|
| `firstName` | `string` | **Required**. First Name|
| `lastName` | `string` | **Required**. Last Name |
| `zip` | `string` | **Required**. Zip Code|
| `birthday` | `string` | **Required**. Date of Birth [DD/MM/YYYY]|


### Payment Transfer
```
  POST /api/auth/signin
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `username` | `string` | **Required**.  Account Name/User Name |
| `password` | `string` | **Required**.  Account Password|

### Deposit Service API'S
```
  POST /account/deposit
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `name` | `string` | **Required**. Account Name/User Name |
| `balance` | `string` | **Required**. Amount to be deposited |

```
http://<IP>:<PORT>/deposit?name=<Account Name>&balance=<Amount>
```
### Payment Transfer
```
  POST /account/transfer
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `from` | `string` | **Required**. Sender Account Name/User Name |
| `to` | `string` | **Required**. Reciever Account Name/User Name |
| `balance` | `string` | **Required**. Amount to be transferred |

```
http://<IP>:<PORT>/account/transfer?from=<Sender Name>&to=<Recipient Name?&balance=<Amount>
```

```
GET /account/get-all
```

```
GET /account/get-transactions
```

```
GET /account/get-transactions-by-name?name=<User Name>
```

## Installation

Install the project using skaffold into docker/kubernetes

```
  git clone <this repo>
  cd bank-of-zero
  skaffold dev
```

After above steps, open a browser and access all the links

* BOZ UI - http://localhost:5500
* Grafana - http://localhost:3000
* Jaeger - http://localhost:16686
## Authors

- Navaneeth Ananthakrishnan
- Ashish Sharma
- Ganapathi Santhanam
- Sridhar Thota


## References

UI Design - Forked from Bank of Anthos [https://github.com/GoogleCloudPlatform/bank-of-anthos]


## Contributing

Contributions are always welcome!

See `contributing.md` for ways to get started.

Please adhere to this project's `code of conduct`.


## License

[MIT](https://choosealicense.com/licenses/mit/)
