/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.health.ServiceHealth;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author luca
 */
public class ConsulCheck {

    public ConsulCheck() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void checkConsul() {

        Consul consul = Consul.builder().withUrl("http://13.90.89.12:8500").build();
        HealthClient healthClient = consul.healthClient();
        // discover only "passing" services
        List<ServiceHealth> services = healthClient.getHealthyServiceInstances("pdfservice").getResponse();
        for (ServiceHealth s : services) {
            System.out.println(s.getService().getAddress());
            System.out.println(s.getService().getPort());

        }
        assertEquals(2,services.size());
    }
}
