/*
 * Copyright 2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.cache;

import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.kork.annotations.Beta;
import com.netflix.spinnaker.moniker.Moniker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public interface OnDemandAgent {
  Logger logger = LoggerFactory.getLogger(OnDemandAgent.class);

  String getProviderName();

  String getOnDemandAgentType();

  // TODO(ttomsu): This seems like it should go in a different interface.
  OnDemandMetricsSupportable getMetricsSupport();

  boolean handles(OnDemandType type, String cloudProvider);

  @Data
  class OnDemandResult {
    String sourceAgentType;
    Collection<String> authoritativeTypes = new ArrayList<>();
    CacheResult cacheResult;
    Map<String, Collection<String>> evictions = new HashMap<>();

    public OnDemandResult() {}

    public OnDemandResult(
        String sourceAgentType,
        CacheResult cacheResult,
        Map<String, Collection<String>> evictions) {
      this.sourceAgentType = sourceAgentType;
      this.cacheResult = cacheResult;
      this.evictions = evictions;
    }
  }

  /*
   * WARNING: this is an interim solution while cloud providers write their own ways to derive monikers.
   */
  default Moniker convertOnDemandDetails(Map<String, String> details) {
    if (details == null || details.isEmpty()) {
      return null;
    }

    try {
      String sequence = details.get("sequence");

      return Moniker.builder()
          .app(details.get("application"))
          .stack(details.get("stack"))
          .detail(details.get("detail"))
          .cluster(details.get("cluster"))
          .sequence(sequence != null ? Integer.valueOf(sequence) : null)
          .build();
    } catch (Exception e) {
      logger.warn("Unable to build moniker", e);
      return null;
    }
  }

  @Nullable
  OnDemandResult handle(ProviderCache providerCache, Map<String, ?> data);

  Collection<Map> pendingOnDemandRequests(ProviderCache providerCache);

  default Map pendingOnDemandRequest(ProviderCache providerCache, String id) {
    Collection<Map> pendingOnDemandRequests = pendingOnDemandRequests(providerCache);
    return pendingOnDemandRequests.stream()
        .filter(m -> id.equals(m.get("id")))
        .findFirst()
        .orElse(null);
  }
}
