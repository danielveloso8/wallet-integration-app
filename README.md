# Wallet Dashboard

A local dashboard for importing Wallet CSV exports and reviewing spending,
income, transfers, and trends. The application stores data in an H2 file
database at `~/.wallet-dashboard/data/wallet`.

## Requirements

- JDK 25
- Maven 3.9+

## Build and run

```shell
mvn clean package
java -jar backend/target/wallet-dashboard.jar
```

Open <http://127.0.0.1:8080>. Back up the database by stopping the application
and copying `~/.wallet-dashboard/data`.

For development, run `mvn -pl backend spring-boot:run
-Dspring-boot.run.profiles=dev` and, in `frontend/`, run `yarn start`.

See [the Wallet CSV contract](docs/wallet-csv-contract.md) for the accepted
export format and import behavior.