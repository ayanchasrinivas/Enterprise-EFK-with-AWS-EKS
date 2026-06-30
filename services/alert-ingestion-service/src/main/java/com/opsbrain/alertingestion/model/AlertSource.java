package com.opsbrain.alertingestion.model;

/** Where the alert originated. Used downstream to decide which APIs to query. */
public enum AlertSource {
    PROMETHEUS,
    GRAFANA,
    CLOUDWATCH,
    KUBERNETES,
    GENERIC
}