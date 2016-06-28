/*
 * Copyright (c) 2016 EMC Corporation. All Rights Reserved.
 */
package com.emc.ia.sdk.sip.ingestion;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.emc.ia.sdk.sip.ingestion.dto.Application;
import com.emc.ia.sdk.sip.ingestion.dto.Applications;
import com.emc.ia.sdk.sip.ingestion.dto.HomeResource;
import com.emc.ia.sdk.sip.ingestion.dto.Link;
import com.emc.ia.sdk.sip.ingestion.dto.Tenant;


public class WhenInitializingArchiveLinkConfigurationParameters {

  public static final String TESTSTRING = "http://identifiers.emc.com/tenant";

  private final Map<String, String> configuration = new HashMap<String, String>();
  private InfoArchiveConfiguration config;

  @Before
  public void init() {
    configuration.put("AuthToken", "XYZ123ABC");
    configuration.put("Application", "Test");
    configuration.put("IAServer", "Test");
    HttpClient client = mock(HttpClient.class);
    config = new InfoArchiveConfigurationImpl(configuration, new TestRestClient(client));
  }

  @Test
  public void shouldInitHeadersDuringObjectCreation() {
    // TODO: Replace by verify(client).setHeaders()
    assertEquals("Headers 'AuthToken' information", config.getHeaders().get(0).toString(), "AuthToken: XYZ123ABC");
    assertEquals("Headers 'Accept' information", config.getHeaders().get(1).toString(), "Accept: application/hal+json");
  }

  @Test
  public void shouldInitTenantDuringObjectCreation() {
    assertNotNull(config.getTenant());
  }

  @Test
  public void shouldInitApplicationDuringObjectCreation() {
    assertNotNull(config.getApplication());
  }

  @Test
  public void shouldInitApisHrefDuringObjectCreation() {
    assertEquals("Headers ApisHref information", config.getAipsHref(), TESTSTRING);
  }

  public static class TestRestClient extends RestClient {

    public TestRestClient(HttpClient client) {
      super(client);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String uri, Class<T> type) {
      T result = null;
      if (type.getName().equals(Tenant.class.getName())) {
        result = (T)new TestTenant();
      } else if (type.getName().equals(HomeResource.class.getName())) {
        result = (T)new TestResource();
      } else if (type.getName().equals(Applications.class.getName())) {
        result = (T)new TestApplications();
      }
      return result;
    }


    public static class TestResource extends HomeResource {

      private final Map<String, Link> links = new HashMap<String, Link>();

      @Override
      public Map<String, Link> getLinks() {
        Link link = new Link();
        link.setHref(TESTSTRING);
        links.put(TESTSTRING, link);
        return links;
      }

    }
  }


  public static class TestTenant extends Tenant {

    private final Map<String, Link> links = new HashMap<String, Link>();

    @Override
    public Map<String, Link> getLinks() {
      Link link = new Link();
      link.setHref(TESTSTRING);
      links.put("http://identifiers.emc.com/applications", link);
      return links;
    }

  }


  public static class TestApplications extends Applications {

    private final Map<String, Link> links = new HashMap<String, Link>();

    @Override
    public Application byName(String name) {
      Application app = new Application();
      Link link = new Link();
      link.setHref(TESTSTRING);
      links.put("http://identifiers.emc.com/aips", link);
      app.setLinks(links);
      return app;
    }

  }

}
