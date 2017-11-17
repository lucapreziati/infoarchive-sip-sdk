/*
 * Copyright (c) 2016-2017 by OpenText Corporation. All Rights Reserved.
 */
package com.opentext.ia.configuration;


/**
 * Build an application.
 * @author Ray Sinnema
 * @since 9.4.0
 *
 * @param <C> The type of configuration to build
 */
public class ApplicationBuilder<C> extends NamedObjectBuilder<TenantBuilder<C>, ApplicationBuilder<C>, C> {

  public ApplicationBuilder(TenantBuilder<C> parent) {
    super(parent, "application");
    setArchiveType("SIP");
    setType("ACTIVE_ARCHIVING");
    setState("IN_TEST");
  }

  private void setType(String type) {
    setProperty("type", type);
  }

  public ApplicationBuilder<C> forAppDecom() {
    setType("APP_DECOMM");
    return this;
  }

  private void setArchiveType(String archiveType) {
    setProperty("archiveType", archiveType);
  }

  public ApplicationBuilder<C> forTables() {
    setArchiveType("TABLE");
    return this;
  }

  private void setState(String state) {
    setProperty("state", state);
  }

  public ApplicationBuilder<C> activated() {
    setState("ACTIVE");
    return this;
  }

  public ApplicationBuilder<C> withDescription(String description) {
    setProperty("description", description);
    return this;
  }

  public ApplicationBuilder<C> withCategory(String category) {
    setProperty("category", category);
    return this;
  }

  public SearchBuilder<C> withSearch() {
    return new SearchBuilder<>(this, "search");
  }

}
