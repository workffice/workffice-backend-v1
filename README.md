# Workffice backend

![workffice-logo.png](workffice-logo.png)

[![CICD](https://github.com/workffice/workffice-backend/actions/workflows/continous-deployment.yml/badge.svg)](https://github.com/workffice/workffice-backend/actions/workflows/continous-deployment.yml)
[![codecov](https://codecov.io/gh/workffice/workffice-backend/branch/main/graph/badge.svg?token=6I9IPXzPOw)](https://codecov.io/gh/workffice/workffice-backend)

## Description
This project is a solution for those office holders who want to
level up their business. The most important features that this
project support are:
- Renting
- Reporting
- Search

## Run project locally
#### Prerequisites
1. Have docker installed
2. Have docker-compose installed

Now you have to:
### Linux
Run the makefile doing `make runserver` and once you are done you have\
tor run `make stopserver`
### Windows
For windows users you have to:
1. Run `gradlew.bat bootJar`
2. Run `docker-compose up`
3. Once you are done run `docker-compose down`

## Development
### Linter
You have to configure your IDE with the
`checkstyle.xml` file located in `workffice-backend/config/checkstyle/checkstyle.xml`

Tips:
- You can follow this [guide](https://somindagamage.medium.com/how-to-configure-checkstyle-and-findbugs-plugins-to-intellij-idea-38148aad2387)
to configure checkstyle on IntelliJ
- The import layout should be this:
```
import all other imports
<blank line>
import java.*
import javax.*
import org.*
<blank line> 
```
- Star import configuration (we don't allow star import)
```
Class count to use import with "*": 100
Name count to use static import with "*": 100
```
### Env variables
For development we use H2 Database and then
you will also need some env variables like
```
SENDGRID_API_KEY=XXX
EMAIL_USERNAME=XXX
EMAIL_PASSWORD=XXX
CLIENT_HOST=http://localhost:3000
```
In case you are missing an env variable
you will see an error like
```
shared.infrastructure.config.EnvironmentConfigurationError: Missing env variables: [${ENV_VARIABLE_NAME}]
```

### Running the project
You can either run the project through your
favourite IDE or you can run it with gradle
doing `gradle bootRun` 

## Testing
- Unit tests: Run `gradle test`
- Generate coverage reports: Run `gradle jacocoTestReport`

## Databases
We have 3 relational databases and one NoSQL.
Each relational database is:
1. Authentication
2. Backoffice
3. Renting
4. The NoSQL one contains the schemas for reporting and search features.

### Local development
For the relational ones we use H2 which can be accessed going to
`localhost:8080/h2-console/` and you can use the following hosts:
1. Authentication:
    * Host: `jdbc:h2:file:./db_authentication`
    * Username: `sa`
    * Password: `sa`
2. Backoffice:
    * Host: `jdbc:h2:file:./db_backoffice`
    * Username: `sa`
    * Password: `sa`
3. Renting:
    * Host: `jdbc:h2:file:./db_renting`
    * Username: `sa`
    * Password: `sa`

<i>Note: If you want to use mysql for local development you are free to do it
just remember to update the host variables in the `application.propperties`</i>

NoSQL is a mongo database:
* Host: `mongodb://mongo-db:27017/workffice`
* Username: `workffice`
* Password: `1234`

### Docker
If you run the docker-compose file then you can access the db
through each db container
1. Authentication:
    * Container name: `workffice-authentication-db`
    * DB: `workffice`
    * Username: `root`
    * Password: `1234`
2. Backoffice:
    * Container name: `workffice-backoffice-db`
    * DB: `workffice`
    * Username: `root`
    * Password: `1234`
3. Renting:
    * Container name: `workffice-renting-db`
    * DB: `workffice`
    * Username: `root`
    * Password: `1234`
    
For mongo we have the following config:

* Container name: `workffice-mongo-db`
* DB: `workffice`
* Username: `workffice`
* Password: `1234`
