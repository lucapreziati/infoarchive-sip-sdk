/*
 * Copyright (c) 2016-2017 by OpenText Corporation. All Rights Reserved.
 */
package com.opentext.ia.yaml.configuration;


class ReplacePdiYamlWithXml extends ReplaceYamlWithXmlContentVisitor {

  ReplacePdiYamlWithXml() {
    super("pdi", "datas", "data", "data");
  }

}
