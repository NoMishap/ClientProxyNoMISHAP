/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imolinfo.sabbioproxy;

import java.io.Closeable;
import java.net.URI;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.simple.SimpleContainerFactory;

/**
 *
 * @author luca
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
    	
        final ResourceConfig resourceConfig = new ResourceConfig(RestServer.class);
              resourceConfig.register(JacksonJsonProvider.class);
              resourceConfig.packages("org.glassfish.jersey.examples.multipart");
        // The following line is to enable GZIP when client accepts it
        try (Closeable server = SimpleContainerFactory.create(new URI("http://0.0.0.0:5555"), resourceConfig)) {
           while(true)
           {
        	   Thread.sleep(3000);;
           }
        }
    }
    
}
