package com.equo.contribution.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.equo.contribution.api.EquoContribution;
import com.equo.contribution.api.EquoContributionBuilder;
import com.equo.contribution.api.IEquoContributionManager;
import com.equo.contribution.api.resolvers.EquoGenericUrlResolver;
import com.equo.contribution.services.pojo.ConfigContribution;
import com.equo.contribution.services.pojo.ContributionSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * A service to define contributions through JSON files.
 */
@Component
public class EquoContributionConfigService implements IContributionConfigService {

  private IEquoContributionManager manager;

  /**
   * Defines a new contribution.
   */
  public List<EquoContribution> defineContributions(JsonObject configJson, Bundle bundle) {
    ArrayList<EquoContribution> contributions = new ArrayList<EquoContribution>();
    Gson parser = new Gson();
    ContributionSet configSet = parser.fromJson(configJson, ContributionSet.class);
    for (ConfigContribution configCont : configSet.getContributions()) {
      contributions.add(parseContributionJsonConfig(configCont, bundle));
    }
    return contributions;
  }

  /**
   * Parses a contribution POJO and creates an Equo contribution with its values.
   */
  public EquoContribution parseContributionJsonConfig(ConfigContribution config, Bundle bundle) {
    if (config.isEmpty()) {
      throw new RuntimeException(
          "A Contribution Config request must be at least one field in the Json config declared.");
    }

    EquoContributionBuilder builder = new EquoContributionBuilder();

    String contributionName = config.getContributionName();
    String contributionHtmlName = config.getContributionHtmlName();
    List<String> proxiedUris = config.getProxiedUris();

    if (contributionName != null) {
      builder.withContributionName(contributionName);
    }
    if (contributionHtmlName != null) {
      builder.withBaseHtmlResource(contributionHtmlName);
    }
    if (proxiedUris != null) {
      for (String uri : proxiedUris) {
        builder.withProxiedUri(uri);
      }
    }

    List<String> scripts = config.getContributedScripts();
    Map<String, String> pathsWithScripts = config.getPathsWithScripts();

    if (scripts != null) {
      builder.withScriptFiles(scripts);
    }
    if (pathsWithScripts != null) {
      for (String path : pathsWithScripts.keySet()) {
        builder.withPathWithScript(path, pathsWithScripts.get(path));
      }
    }
    return builder.withManager(manager).withUrlResolver(
        new EquoGenericUrlResolver(bundle.adapt(BundleWiring.class).getClassLoader())).build();
  }

  @Reference
  public void setManager(IEquoContributionManager manager) {
    this.manager = manager;
  }

  public void unsetManager(IEquoContributionManager server) {
    this.manager = null;
  }

}
