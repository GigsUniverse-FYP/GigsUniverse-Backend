# Project Description

This is a Backend Section for the GigsUniverse Application.

FYP Topic: `The Impact of the Gig Economy on Youth Employment and Career Growth`

Developed By: Soh Jia Seng TP065158

A microservice architecture is implemented to efficiently managing every services.

## Command
- This project utilizes `maven` to handle relevant dependencies 


Download Dependencies:
```bash
mvn clean install
```

Local Development:
```bash
mvn spring-boot:run
```

Compilation:
```bash
mvn package
```

Running Compiled Program:
```bash
java -jar target/<your-app-name>-0.0.1-SNAPSHOT.jar
```

## Setup
Note: `This Application by Default Enforces HTTPS`

- Refer /src/main/resources/application.properties.example for environment setup.
- Refer /src/main/resources/.env and fill up relevant details to run the docker image with correct setup.
- This given setup is able to support tunneling or deployment on server. 

## Docker
Windows Command:
```bash
docker build ^
  -t giguniverse-backend ^
  .
```

Linux / MasOS Command:
```bash
docker build \
  -t giguniverse-backend \
  .
```

Run with Real .env Setup
```bash
docker run -d -p 8080:8080 --env-file .env giguniverse-backend
```
