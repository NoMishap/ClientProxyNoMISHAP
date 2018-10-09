/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imolinfo.sabbioproxy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
//import java.util.logging.Level;
//import java.util.logging.Logaager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.health.ServiceHealth;
import co.elastic.apm.api.ElasticApm;
//import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;

/**
 *
 * @author luca
 */
@Path("/x")
public class RestServer {

	private static Cache<String, List> consulCache;
	private static Consul consul;
	private static String ConsulIP = (System.getenv("CONSUL_IP")==null) ? "http://127.0.0.1:8500" : System.getenv("CONSUL_IP");
	static {
		System.out.println(ConsulIP);
		CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.withCache("consulCache",
						CacheConfigurationBuilder
								.newCacheConfigurationBuilder(String.class, List.class, ResourcePoolsBuilder.heap(100))
								.withExpiry(Expirations.timeToLiveExpiration(Duration.of(15L, TimeUnit.SECONDS))))
				.build();
		cacheManager.init();

		consulCache = cacheManager.getCache("consulCache", String.class, List.class);
		consul = Consul.builder().withUrl(ConsulIP).build();

	}

	@GET
	@Path("/pdfToText")
	@Produces(MediaType.APPLICATION_JSON)
	public Response convertPDF() {
		HealthClient healthClient = consul.healthClient();
		List<ServiceHealth> services = null;
		List<ServiceHealth> servicesList = consulCache.get("servicesCache");
        System.out.println(servicesList);

        if (servicesList == null) {
			services = healthClient.getHealthyServiceInstances("pdfservice").getResponse();
			consulCache.put("servicesCache", services);
		}
		else {
			services = new ArrayList<ServiceHealth>(servicesList);	
		}
		
		if (services.size() > 0) {
			Collections.shuffle(services);
			Iterator<ServiceHealth> iterator = services.iterator();
			while (iterator.hasNext()) {
				final ServiceHealth currentService = iterator.next();
				final String serviceName = determineServiceName(currentService);
				ServiceInvocationResult result = invokeService(currentService, serviceName, 0);
				if (result.getResponse().getStatus() == Status.OK.getStatusCode()) 
				{
					return result.getResponse();
				}
			}

		}

		return Response.serverError().build();

	}

	private ServiceInvocationResult invokeService(final ServiceHealth service, final String serviceName,
			final long timeStart) {


		try {
			StringBuilder response = new StringBuilder();

			String url = service.getService().getAddress();
			URL obj = new URL(url);
			HttpURLConnection con;
			
			Transaction transaction = ElasticApm.startTransaction();
			try {
			    transaction.setName("MyController#myAction");
			    transaction.setType(Transaction.TYPE_REQUEST);
			    transaction.addTag("paasName", serviceName);
			    
			    con = (HttpURLConnection) obj.openConnection();
				con.getContent().toString();
				try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
					String inputLine;

					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
				}
			    
			} catch (Exception e) {
			    transaction.captureException(e);
			    throw e;
			} finally {
			    transaction.end();
			}

			return new ServiceInvocationResult(Response.ok(response.toString()).build(),0);

		} catch (Exception ex) {

			return new ServiceInvocationResult(Response.serverError().build(),0);
		}
	}

	private String determineServiceName(final ServiceHealth service) {
		String servizioScelto = null;
		String url = service.getService().getAddress();
		StringTokenizer st = new StringTokenizer(url, "/");
		st.nextToken();
		servizioScelto = st.nextToken();
		return servizioScelto;
	}

}
