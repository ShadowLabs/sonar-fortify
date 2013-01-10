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
package org.sonar.plugins.fortify.batch;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Violation;
import org.sonar.plugins.fortify.base.FortifyRuleRepository;
import org.sonar.plugins.fortify.client.FortifyClient;
import org.sonar.plugins.fortify.client.IssueWrapper;

import java.util.Collection;
import java.util.List;

public class IssueSensor implements Sensor {
  private final FortifyClient client;
  private final FortifyProject fortifyProject;
  private final RulesProfile profile;
  private ResourceMatcher resourceMatcher;

  public IssueSensor(FortifyClient client, FortifyProject fortifyProject, RulesProfile profile) {
    this(client, fortifyProject, profile, new ResourceMatcher());
  }

  @VisibleForTesting
  IssueSensor(FortifyClient client, FortifyProject fortifyProject, RulesProfile profile, ResourceMatcher resourceMatcher) {
    this.client = client;
    this.fortifyProject = fortifyProject;
    this.profile = profile;
    this.resourceMatcher = resourceMatcher;
  }

  public boolean shouldExecuteOnProject(Project project) {
    List<ActiveRule> activeRules = profile.getActiveRulesByRepository(FortifyRuleRepository.fortifyRepositoryKey(project.getLanguageKey()));
    return fortifyProject.exists() && !activeRules.isEmpty();
  }

  public void analyse(Project project, SensorContext sensorContext) {
    String repositoryKey = FortifyRuleRepository.fortifyRepositoryKey(project.getLanguageKey());

    // TODO optimization : load only the issues on enabled rules -> that would prevent from requesting details on issues to be ignored.
    // But that's not possible through Fortify SOAP services
    Collection<IssueWrapper> issues = client.getIssues(fortifyProject.getVersionId());
    LoggerFactory.getLogger(IssueSensor.class).info("Loading " + issues.size() + " Fortify issues");
    for (IssueWrapper issue : issues) {
      ActiveRule activeRule = profile.getActiveRuleByConfigKey(repositoryKey, issue.getRuleConfigKey());
      if (activeRule != null) {
        Resource resource = resourceMatcher.resourceOf(issue, project.getFileSystem());
        if (sensorContext.isIndexed(resource, false)) {
          Violation violation = Violation.create(activeRule, resource);
          violation.setLineId(issue.getLine());
          violation.setMessage(issue.getTextAbstract());
          sensorContext.saveViolation(violation);
        }
      }
    }

  }

  @Override
  public String toString() {
    return "Fortify Issues";
  }


  static class ResourceMatcher {
    Resource resourceOf(IssueWrapper issue, ProjectFileSystem fileSystem) {
      // hack for java... still have to be fixed in sonar core
      java.io.File file = getFile(fileSystem.getBasedir(), issue.getFilePath());
      if (Java.isJavaFile(file)) {
        return new JavaFile(issue.getPackageName(), issue.getClassName());
      }
      return File.fromIOFile(file, fileSystem.getSourceDirs());
    }
    
	/**
	 * Checks for overlap between the baseDir and the relativePath, and
	 * combines them removing duplicated folders
	 * 
	 * @param baseDir The absolute base path
	 * @param filePath the relative path to the file
	 * @return the combined path with any duplicate folders removed
	 */
    @VisibleForTesting
    java.io.File getFile(java.io.File baseDir, String filePath) {
      String childPath = StringUtils.stripStart(filePath, "/");
      
      String reverseBasePath = StringUtils.reverseDelimited(baseDir.getPath(), java.io.File.separatorChar);
      if(java.io.File.separatorChar != '/') {
        reverseBasePath = reverseBasePath.replace(java.io.File.separatorChar, '/');
      }
      reverseBasePath = StringUtils.stripStart(reverseBasePath, "/");

      // find the overlap and remove it from the childPath
      int indexOfDiff = StringUtils.indexOfDifference(reverseBasePath,
          childPath);
      if (indexOfDiff > 0) {
        while (indexOfDiff > 0) {
          if (childPath.charAt(indexOfDiff) == '/') {
            break;
          }
          indexOfDiff--;
        }
        childPath = childPath.substring(indexOfDiff);
      }
      
      return new java.io.File(baseDir, childPath);
    }
  }
}