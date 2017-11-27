/*
 * Copyright (c) 2016-2017 by OpenText Corporation. All Rights Reserved.
 */
package com.opentext.ia.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.opentext.ia.configuration.JsonConfigurationProducer.JsonConfiguration;


public class WhenBuildingConfigurations {

  private static final Pattern NAME_PATTERN = Pattern.compile("[a-z]{1,3}(?<uuid>.*)");
  private static final String NAME = "name";
  private static final String TYPE = "type";
  private static final String DESCRIPTION = "description";
  private static final String DEFAULT_TENANT_NAME = "INFOARCHIVE";
  private static final String TENANT_NAME = "myTenant";
  private static final String APPLICATION_NAME = "myApplication";
  private static final String SEARCH_NAME = "mySearch";
  private static final String DESCRIPTIVE_TEXT = "myDescription";
  private static final String CATEGORY = "myCategory";
  private static final String STATE = "state";
  private static final String DRAFT = "DRAFT";
  private static final String FILE_SYSTEM_ROOT_NAME = "myFileSystemRoot";
  private static final String PATH = "path";
  private static final String SOME_PATH = "/path/to/some/place";
  private static final String SPACE_NAME = "mySpace";
  private static final String SPACE_ROOT_XDB_LIBRARY_NAME = "mySpaceRootXdbLibrary";
  private static final String XDB_LIBRARY_NAME = "myXdbLibrary";

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private final ConfigurationProducer<JsonConfiguration> producer = new JsonConfigurationProducer();
  private final ConfigurationBuilder<JsonConfiguration> builder = new ConfigurationBuilder<>(producer);
  private Configuration<ConfigurationObject> configuration;

  @Test
  public void shouldUseDefaultPropertiesForTenant() {
    configuration = builder.withTenant().build();

    assertEquals(NAME, DEFAULT_TENANT_NAME, configuration.getTenant().getProperties().getString(NAME));
  }

  @Test
  public void shouldSetPropertiesOfTenant() {
    configuration = builder.withTenant()
        .named(TENANT_NAME)
    .build();

    assertEquals(NAME, TENANT_NAME, nameOf(configuration.getTenant()));
  }

  private String nameOf(ConfigurationObject object) {
    return object.getProperties().getString(NAME);
  }

  @Test
  public void shouldUseDefaultPropertiesForApplication() {
    configuration = builder.withApplication().build();
    ConfigurationObject application = configuration.getApplication();

    assertRandomName(application);
    assertProperties(application,
        "tenant", DEFAULT_TENANT_NAME,
        TYPE, "ACTIVE_ARCHIVING",
        "archiveType", "SIP",
        STATE, "IN_TEST");
  }

  private void assertRandomName(ConfigurationObject actual) {
    Matcher matcher = NAME_PATTERN.matcher(nameOf(actual));
    if (matcher.matches()) {
      assertUuid(NAME, matcher.group("uuid"));
    } else {
      fail("Not a random name");
    }
  }

  private void assertUuid(String message, String actual) {
    try {
      UUID.fromString(actual);
    } catch (IllegalArgumentException e) {
      fail(message + " is not a UUID");
    }
  }

  private void assertProperties(ConfigurationObject actual, String... expectedPropertyValues) {
    JSONObject properties = actual.getProperties();
    for (int i = 0; i < expectedPropertyValues.length; i += 2) {
      String property = expectedPropertyValues[i];
      assertEquals(property, expectedPropertyValues[i + 1], properties.optString(property));
    }
  }

  @Test
  public void shouldSetPropertiesOfApplication() {
    configuration = builder.withTenant()
        .named(TENANT_NAME)
        .withApplication()
            .named(APPLICATION_NAME)
            .forAppDecom()
            .forTables()
            .activated()
            .withDescription(DESCRIPTIVE_TEXT)
            .withCategory(CATEGORY)
    .build();

    ConfigurationObject application = configuration.getApplication();
    assertProperties(application,
        "tenant", TENANT_NAME,
        NAME, APPLICATION_NAME,
        TYPE, "APP_DECOMM",
        "archiveType", "TABLE",
        STATE, "ACTIVE",
        DESCRIPTION, DESCRIPTIVE_TEXT,
        "category", CATEGORY);
  }

  @Test
  public void shouldThrowExceptionWhenAskedForMissingItem() {
    thrown.expect(IllegalArgumentException.class);

    builder.withTenant().build().getApplication();
  }

  @Test
  public void shouldUseDefaultPropertiesForSearch() {
    configuration = builder.withSearch().build();
    ConfigurationObject search = configuration.getSearch();

    assertRandomName(search);
    assertProperties(search,
        STATE, DRAFT);
  }

  @Test
  public void shouldSetPropertiesForSearch() {
    configuration = builder.withApplication()
        .named(APPLICATION_NAME)
        .withSearch()
            .named(SEARCH_NAME)
            .withDescription(DESCRIPTIVE_TEXT)
            .published()
    .build();
    ConfigurationObject search = configuration.getSearch();

    assertProperties(search,
        "application", APPLICATION_NAME,
        NAME, SEARCH_NAME,
        STATE, "PUBLISHED",
        DESCRIPTION, DESCRIPTIVE_TEXT);
  }

  @Test
  public void shouldUseDefaultPropertiesForFileSystemRoot() {
    configuration = builder.withFileSystemRoot().build();
    ConfigurationObject fileSystemRoot = configuration.getFileSystemRoot();

    assertRandomName(fileSystemRoot);
    assertProperties(fileSystemRoot,
        PATH, "/data/root",
        TYPE, "FILESYSTEM");
  }

  @Test
  public void shouldSetPropertiesForFileSystemRoot() {
    configuration = builder.withFileSystemRoot()
        .named(FILE_SYSTEM_ROOT_NAME)
        .withDescription(DESCRIPTIVE_TEXT)
        .at(SOME_PATH)
        .onIsilon()
    .build();
    ConfigurationObject fileSystemRoot = configuration.getFileSystemRoot();

    assertProperties(fileSystemRoot,
        NAME, FILE_SYSTEM_ROOT_NAME,
        DESCRIPTION, DESCRIPTIVE_TEXT,
        PATH, SOME_PATH,
        TYPE, "ISILON");
  }

  @Test
  public void shouldUseDefaultPropertiesForSpace() {
    configuration = builder.withSpace().build();
    ConfigurationObject space = configuration.getSpace();

    assertRandomName(space);
  }

  @Test
  public void shouldSetPropertiesForSpace() {
    configuration = builder.withApplication()
        .named(APPLICATION_NAME)
        .withSpace()
            .named(SPACE_NAME)
    .build();
    ConfigurationObject space = configuration.getSpace();

    assertProperties(space,
        "application", APPLICATION_NAME,
        NAME, SPACE_NAME);
  }

  @Test
  public void shouldUseDefaultPropertiesForSpaceRootXdbLibrary() {
    configuration = builder.withSpace()
        .withSpaceRootXdbLibrary()
    .build();
    ConfigurationObject spaceRootXdbLibrary = configuration.getSpaceRootXdbLibrary();

    assertRandomName(spaceRootXdbLibrary);
  }

  @Test
  public void shouldSetPropertiesForSpaceRootXdbLibrary() {
    configuration = builder.withSpace()
        .named(SPACE_NAME)
        .withSpaceRootXdbLibrary()
            .named(SPACE_ROOT_XDB_LIBRARY_NAME)
    .build();
    ConfigurationObject spaceRootXdbLibrary = configuration.getSpaceRootXdbLibrary();

    assertProperties(spaceRootXdbLibrary,
        "space", SPACE_NAME,
        NAME, SPACE_ROOT_XDB_LIBRARY_NAME);
  }

  @Test
  public void shouldUseDefaultPropertiesForXdbLibrary() {
    configuration = builder.withSpace()
        .withSpaceRootXdbLibrary()
            .withXdbLibrary()
    .build();
    ConfigurationObject xdbLibrary = configuration.getXdbLibrary();

    assertRandomName(xdbLibrary);
  }

  @Test
  public void shouldSetPropertiesForXdbLibrary() {
    configuration = builder.withSpace()
        .named(SPACE_NAME)
        .withSpaceRootXdbLibrary()
            .named(SPACE_ROOT_XDB_LIBRARY_NAME)
            .withXdbLibrary()
                .named(XDB_LIBRARY_NAME)
    .build();
    ConfigurationObject xdbLibrary = configuration.getXdbLibrary();

    assertProperties(xdbLibrary,
        "parentSpaceRootXdbLibrary", SPACE_ROOT_XDB_LIBRARY_NAME,
        NAME, XDB_LIBRARY_NAME);
  }

}
