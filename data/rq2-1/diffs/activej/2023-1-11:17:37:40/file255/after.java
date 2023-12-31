import io.activej.http.AsyncServlet;
import io.activej.http.HttpResponse;
import io.activej.http.Servlet_Routing;
import io.activej.inject.annotation.Named;
import io.activej.inject.annotation.Provides;
import io.activej.launchers.http.HttpServerLauncher;

import java.util.concurrent.ThreadLocalRandom;

/**
 * An example of setting routes that change at random.
 * <p>
 * You may test server behaviour by issuing accessing <a href="http://localhost:8080">the server</a> from a browser
 */
public final class DynamicRoutingExample extends HttpServerLauncher {

	@Provides
	AsyncServlet mainServlet(@Named("First") AsyncServlet firstServlet, @Named("Second") AsyncServlet secondServlet) {
		return Servlet_Routing.create()
				.map("/*", request -> {
					if (ThreadLocalRandom.current().nextBoolean()) {
						return firstServlet.serve(request);
					} else {
						return secondServlet.serve(request);
					}
				});
	}

	@Provides
	@Named("First")
	AsyncServlet firstServlet() {
		return request -> HttpResponse.ok200().withHtml(
				"<h1>This page is served by first servlet</h1>" +
						"<h3>Try to reload the page</h3>"
		);
	}

	@Provides
	@Named("Second")
	AsyncServlet secondServlet() {
		return request -> HttpResponse.ok200().withHtml(
				"<h1>This page is served by second servlet</h1>" +
						"<h3>Try to reload the page</h3>"
		);
	}

	public static void main(String[] args) throws Exception {
		new DynamicRoutingExample().launch(args);
	}
}
