package com.equo.contribution.services.pojo;

import java.util.List;

/**
 * POJO for JSON config file.
 */
public class ContributionSet {

  List<ConfigContribution> contributions;

  public List<ConfigContribution> getContributions() {
    return contributions;
  }

  public void setContributions(List<ConfigContribution> contributions) {
    this.contributions = contributions;
  }

}
