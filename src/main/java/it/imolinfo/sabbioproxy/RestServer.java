/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imolinfo.sabbioproxy;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.health.ServiceHealth;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

/**
 *
 * @author luca
 */
@Path("/x")
public class RestServer {

	private static Cache<String, List> consulCache;
	private static Consul consul;
	static {
		CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.withCache("consulCache",
						CacheConfigurationBuilder
								.newCacheConfigurationBuilder(String.class, List.class, ResourcePoolsBuilder.heap(100))
								.withExpiry(Expirations.timeToLiveExpiration(Duration.of(15L, TimeUnit.SECONDS))))
				.build();
		cacheManager.init();

		consulCache = cacheManager.getCache("consulCache", String.class, List.class);
		try {
			if (InetAddress.getLocalHost().getHostName().equals("consul")){
				consul = Consul.builder().withUrl("http://127.0.0.1:8500").build();
			}
			else
				consul = Consul.builder().withUrl("http://13.90.89.12:8500").build();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			consul = Consul.builder().withUrl("http://13.90.89.12:8500").build();
		}

	}
	private static final StatsDClient statsd = new NonBlockingStatsDClient(
			"", /* prefix to any stats; may be null or empty string */
			"localhost", /* common case: localhost */
			8125, /* port */
			new String[] { "tag:value" } /*
											 * DataDog extension: Constant tags,
											 * always applied
											 */
	);

	@GET
	@Path("/pdfservice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response convertPDF() {
		statsd.incrementCounter("proxy_client.numero_chiamate");
		final long timeStart = System.currentTimeMillis();
		long serviceInvocationAccumulator = 0L;

		HealthClient healthClient = consul.healthClient();

		List<ServiceHealth> services = null;
		List servicesList = consulCache.get("servicesCache");
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
				// aumento l'indice delle chiamate al singolo paas nel log
				statsd.incrementCounter("paas.invocation_count", serviceName);

				ServiceInvocationResult result = invokeService(currentService, serviceName, timeStart);
				serviceInvocationAccumulator = serviceInvocationAccumulator + result.getExecutionTime();
				if (result.getResponse().getStatus() == Status.OK.getStatusCode()) {
					statsd.recordExecutionTime("broker_overhead",
							(System.currentTimeMillis() - timeStart) - serviceInvocationAccumulator, "");
					statsd.recordExecutionTime("broker_overall_time",
							(System.currentTimeMillis() - timeStart),"");
					return result.getResponse();
				}
			}

		}

		statsd.recordExecutionTime("broker_overhead",
				(System.currentTimeMillis() - timeStart) - serviceInvocationAccumulator, "");
		return Response.serverError().build();

	}

	private ServiceInvocationResult invokeService(final ServiceHealth service, final String serviceName,
			final long timeStart) {

		long timeBeforeInvocation = 0L;
		try {
			StringBuilder response = new StringBuilder();

			String url = service.getService().getAddress();
			URL obj = new URL(url);
			HttpURLConnection con;
			// Timestamp di inizio chiamata servizio
			timeBeforeInvocation = System.currentTimeMillis();
			con = (HttpURLConnection) obj.openConnection();

			// loggo il codice di risposa
			statsd.incrementCounter("responseCode_" + con.getResponseCode());
			// loggo il codice di risposta per singolo paas service
			statsd.incrementCounter("responseCode_" + serviceName + "_" + con.getResponseCode());

			con.getContent().toString();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String inputLine;

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
			}

			// Timestamp di fine chiamata servizio
			long timeAfterInvocation = System.currentTimeMillis();
			// procedura di scrittura su log dei tempi
			statsd.recordExecutionTime("adapter_client_overhead", timeBeforeInvocation - timeStart, "");
			statsd.recordExecutionTime("adapter_client_service_invocation", timeAfterInvocation - timeBeforeInvocation,
					"");
			statsd.recordExecutionTime("adapter_client_overhead_" + serviceName, timeBeforeInvocation - timeStart, "");
			statsd.recordExecutionTime("adapter_client_service_invocation_" + serviceName,
					timeAfterInvocation - timeBeforeInvocation, "");

			return new ServiceInvocationResult(Response.ok(response.toString()).build(),
					timeAfterInvocation - timeBeforeInvocation);

		} catch (Exception ex) {
			Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
			ex.printStackTrace();
			statsd.incrementCounter("paas.failure_count", serviceName);
			return new ServiceInvocationResult(Response.serverError().build(),
					System.currentTimeMillis() - timeBeforeInvocation);
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
