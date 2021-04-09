# Vertx Transaction Error Reproducer
When creating a transaction and running multiple commands simultaneously if an error occurs the
error message will not be returned, but the error message "Transaction already completed." will
be returned instead. This makes it difficult to trace the source of the issue.

I haven't looked at the code yet and am strapped for time right now or else would love to debug this.
Maybe someone has an intuition as to why this is happening.

This runs multiple commands within a transaction, concurrently, and one fails. The failing
command does not return its error message though, and instead says `transaction closed`.

```java
pgClient.getConnection().flatMap(sqlConnection -> {
	return sqlConnection.begin().flatMap(trans -> {
	futureList.add(sqlConnection.query("SELECT table_name FROM information_schema.tables")
	.execute());
	futureList.add(sqlConnection.query("SELECT dne FROM information_schema.tables")
	.execute());
	return CompositeFuture.all(futureList)
	.eventually(v -> sqlConnection.close());
	});
	}).onSuccess(res -> {
	System.out.println("Shouldn't see me.");
	}).onFailure(Throwable::printStackTrace); // will print the expected/desired error message
```

```java
pgClient.getConnection().flatMap(sqlConnection -> {
	return sqlConnection.begin().flatMap(trans -> {
	futureList2.add(sqlConnection.query("SELECT table_name FROM information_schema.tables")
	.execute());
	futureList2.add(sqlConnection.query("SELECT dne FROM information_schema.tables")
	.execute());
	futureList2.add(sqlConnection.query("SELECT table_name FROM information_schema.tables")
	.execute());
	futureList2.add(sqlConnection.query("SELECT table_name FROM information_schema.tables")
	.execute());
	return CompositeFuture.all(futureList2)
	.eventually(v -> sqlConnection.close()); // will print transaction closed
	});
	}).onSuccess(res -> {
	System.out.println("Shouldn't see me.");
	}).onFailure(Throwable::printStackTrace);
```


## Reproducer
`docker run --rm -d -e POSTGRES_PASSWORD=password -p 33333:5432 postgres:11.11`

`./gradlew run`

You'll see two error messages printed out:
1. When you don't try to execute any additional statements you'll receive the error that caused
   the transaction to fail: `io.vertx.pgclient.PgException: { "message": "column \"dne\" does not exist", "severity": "ERROR", "code": "42703", "position": "8", "file": "parse_relation.c", "line": "3294", "routine": "errorMissingColumn" }`.
2. When you execute additional statements you won't receive the initially failing error message,
   an error message that the transaction has already completed: `io.vertx.core.VertxException: Transaction already completed`.
