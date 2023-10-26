package org.acme;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/hello")
public class GreetingResource {

    private static final String FALL_BACK_MESSAGE = "FallbackMethod: ";
    private static int NUMBER = 0;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from RESTEasy Reactive";
    }

    @GET
    @Path("/fallback/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Retry(maxRetries = 3, delay = 1000)
    @Fallback(fallbackMethod = "recover")
    @Timeout(7000)
    public String getName(@PathParam("name") String name) {

        if (name.equalsIgnoreCase("error")) {
            ResponseBuilderImpl builder = new ResponseBuilderImpl();
            builder.status(Response.Status.INTERNAL_SERVER_ERROR);
            builder.entity("Error during request");
            Response response = builder.build();
            throw new WebApplicationException(response);
        }

        return name;
    }

    @GET
    @Path("/circuit")
    @CircuitBreaker(requestVolumeThreshold = 2)
    public Response circuit() {

        System.out.println("request number: " + NUMBER++);

        try {
            if (NUMBER > 2) {
                NUMBER = 0;
                throw new WebApplicationException();
            }
            return Response.status(200).entity("You're able to reach me. But only two times before CB open. Attempt " + NUMBER).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Too many requests CB. Wait a minute!").build();
        }
    }

    public String recover(String name) {
        return FALL_BACK_MESSAGE + name;
    }
}
