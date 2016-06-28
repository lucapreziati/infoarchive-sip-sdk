/*
 * Copyright (c) 2016 EMC Corporation. All Rights Reserved.
 */
package com.emc.ia.sdk.sip.ingestion;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.emc.ia.sdk.sip.ingestion.dto.Application;
import com.emc.ia.sdk.sip.ingestion.dto.Applications;
import com.emc.ia.sdk.sip.ingestion.dto.HomeResource;
import com.emc.ia.sdk.sip.ingestion.dto.IngestionResponse;
import com.emc.ia.sdk.sip.ingestion.dto.Link;
import com.emc.ia.sdk.sip.ingestion.dto.LinkContainer;
import com.emc.ia.sdk.sip.ingestion.dto.ReceptionResponse;
import com.emc.ia.sdk.sip.ingestion.dto.Tenant;
import com.emc.ia.sdk.support.io.RuntimeIoException;


public class WhenInitializingArchiveLinkConfigurationParameters {

  private static final String BILLBOARD_URI = "http://foo.com/bar";
  private static final String APPLICATION_NAME = "Test";

  private final Map<String, Link> links = new HashMap<String, Link>();
  private final Map<String, String> configuration = new HashMap<String, String>();
  private final InfoArchiveRestClient client = new InfoArchiveRestClient();
  private RestClient restClient;
  private HomeResource resource;
  private Tenant tenant;
  private Applications applications;
  private Application application;

  @Before
  public void init() throws IOException {
    configuration.put("AuthToken", "XYZ123ABC");
    configuration.put("Application", APPLICATION_NAME);
    configuration.put("IAServer", BILLBOARD_URI);
    restClient = mock(RestClient.class);
    resource = new HomeResource();
    Link link = mock(Link.class);
    tenant = new Tenant();
    application = new Application();
    applications = mock(Applications.class);
    client.setRestClient(restClient);

    links.put(InfoArchiveRestClient.LINK_TENANT, link);
    links.put(InfoArchiveRestClient.LINK_APPLICATIONS, link);
    links.put(InfoArchiveRestClient.LINK_AIPS, link);
    links.put(InfoArchiveRestClient.LINK_INGEST, link);
    resource.setLinks(links);
    tenant.setLinks(links);
    application.setLinks(links);

    when(restClient.get(BILLBOARD_URI, HomeResource.class)).thenReturn(resource);
    when(link.getHref()).thenReturn(BILLBOARD_URI);
    when(restClient.follow(any(LinkContainer.class), anyString(), eq(Tenant.class))).thenReturn(tenant);
    when(restClient.follow(any(LinkContainer.class), anyString(), eq(Applications.class))).thenReturn(applications);
    when(applications.byName(APPLICATION_NAME)).thenReturn(application);
    when(restClient.refresh(applications)).thenReturn(applications);
  }

  @Test
  public void shouldInitHeadersDuringObjectCreation() throws IOException {
    client.configure(configuration);

    verify(restClient).get(BILLBOARD_URI, HomeResource.class);
    verify(restClient).follow(resource, InfoArchiveRestClient.LINK_TENANT, Tenant.class);
    verify(restClient).follow(tenant, InfoArchiveRestClient.LINK_APPLICATIONS, Applications.class);
    verify(applications).byName(APPLICATION_NAME);
  }

  @Test (expected = RuntimeException.class)
  public void shouldThrowExceptionWileConfiguring() {
    client.configure(null);
  }

  @SuppressWarnings("unchecked")
  @Test (expected = RuntimeIoException.class)
  public void shouldWrapExceptionDuringConfiguration() throws IOException {
    when(restClient.get(BILLBOARD_URI, HomeResource.class)).thenThrow(IOException.class);
    client.configure(configuration);
  }

  @Test
  public void shouldIngestSuccessfully() throws IOException {
    client.configure(configuration);

    String source = "This is the source of my input stream";
    InputStream sip = IOUtils.toInputStream(source, "UTF-8");

    ReceptionResponse receptionResponse = new ReceptionResponse();
    IngestionResponse ingestionResponse = mock(IngestionResponse.class);
    receptionResponse.setLinks(links);
    when(restClient.ingest(anyString(), eq(sip), eq(ReceptionResponse.class))).thenReturn(receptionResponse);
    when(restClient.put(anyString(), eq(IngestionResponse.class))).thenReturn(ingestionResponse);
    when(ingestionResponse.getAipId()).thenReturn("sip001");

    assertEquals(client.ingest(sip), "sip001");
  }

  @Test(expected = RuntimeException.class)
  public void ingestShouldThrowRuntimeExceptionWhenConfigureIsNotInvoked() throws IOException {
    String source = "This is the source of my input stream";
    InputStream sip = IOUtils.toInputStream(source, "UTF-8");
    client.ingest(sip);
  }

  @Test(expected = RuntimeException.class)
  public void ingestShouldThrowRuntimeExceptionWhenSipIsNull() throws IOException {
    client.ingest(null);
  }

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionWhenConfigurationParametersAreNull() throws IOException {
    Map<String, String> config = new HashMap<String, String>();
    client.configure(config);
  }

  @Test
  public void shouldCreateApplicationWhenNotFound() throws IOException {
    final AtomicBoolean afterCreate = new AtomicBoolean(false);
    when(applications.byName(APPLICATION_NAME)).thenAnswer(invocation -> {
      if (afterCreate.get()) {
        return application;
      } else {
        afterCreate.set(true);
        return null;
      }
    });

    client.configure(configuration);

    verify(restClient).createCollectionItem(eq(applications), any(Application.class));
  }

}
