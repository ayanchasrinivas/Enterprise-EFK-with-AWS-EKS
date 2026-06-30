package com.opsbrain.contextcollector.model;

import lombok.Builder;
import lombok.Value;

/** A Grafana dashboard deep-link relevant to the alert — for the human engineer. */
@Value
@Builder
public class DashboardLink {
    String title;
    String url;
}