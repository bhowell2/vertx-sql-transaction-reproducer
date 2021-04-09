package VertxSqlTransactionErrorReproducer;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

import java.util.ArrayList;
import java.util.List;

public class App {

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		PgConnectOptions connectOptions = new PgConnectOptions()
			.setPort(33333)
			.setUser("postgres")
			.setPassword("password")
			.setHost("localhost")
			.setDatabase("postgres");
		PoolOptions poolOptions = new PoolOptions();
		PgPool pgClient = PgPool.pool(vertx, connectOptions, poolOptions);

		List<Future> futureList = new ArrayList<>();

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

		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			// dont care
			throw new RuntimeException(e);
		}

		List<Future> futureList2 = new ArrayList<>();

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

		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			// dont care
		} finally {
			vertx.close();
		}

	}

}
