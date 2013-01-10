/*
 * Sonar Fortify Plugin
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.fortify;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.fortify.base.FortifyConstants;
import org.sonar.plugins.fortify.base.FortifyMetrics;
import org.sonar.plugins.fortify.base.FortifyRuleRepositories;
import org.sonar.plugins.fortify.batch.AuditContextSensor;
import org.sonar.plugins.fortify.batch.FortifyProject;
import org.sonar.plugins.fortify.batch.IssueSensor;
import org.sonar.plugins.fortify.batch.PerformanceIndicatorSensor;
import org.sonar.plugins.fortify.client.FortifyClient;
import org.sonar.plugins.fortify.ui.FortifyWidget;

import java.util.Arrays;
import java.util.List;

@Properties({
  // connection properties
  @Property(key = FortifyConstants.PROPERTY_URL, name = "SSC URL"),
  @Property(key = FortifyConstants.PROPERTY_LOGIN, name = "SSC Login"),
  @Property(key = FortifyConstants.PROPERTY_PASSWORD, name = "SSC Password", type = PropertyType.PASSWORD),
  @Property(key = FortifyConstants.PROPERTY_ENABLE, name = "Enable Fortify", type = PropertyType.BOOLEAN, defaultValue = "false",
    description = "Enable import of Fortify audit report", global = true, project = true),
  @Property(key = FortifyConstants.PROPERTY_REPORT_LINK_ENABLED, name = "Enable Report Link", type = PropertyType.BOOLEAN,
    defaultValue = "true", description = "Enable the 'Open Report' link on the Fortify widget"),

  // optional project properties
  @Property(key = FortifyConstants.PROPERTY_PROJECT_NAME, name = "Fortify Project Name", global = false, project = true),
  @Property(key = FortifyConstants.PROPERTY_PROJECT_VERSION, name = "Fortify Project Version", global = false, project = true)
})
public final class FortifyPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(FortifyMetrics.class, FortifyRuleRepositories.class, FortifyClient.class,
      FortifyProject.class, AuditContextSensor.class, PerformanceIndicatorSensor.class, IssueSensor.class,
      FortifyWidget.class);
  }

}
