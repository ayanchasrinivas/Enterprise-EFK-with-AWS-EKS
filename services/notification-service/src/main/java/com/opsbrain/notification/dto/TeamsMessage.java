package com.opsbrain.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamsMessage {

    @JsonProperty("@type")
    private String type;

    @JsonProperty("@context")
    private String context;

    private String summary;
    private String themeColor;

    private List<Section> sections;

    @JsonProperty("potentialAction")
    private List<Action> potentialActions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Section {
        @JsonProperty("activityTitle")
        private String activityTitle;

        @JsonProperty("activitySubtitle")
        private String activitySubtitle;

        @JsonProperty("activityImage")
        private String activityImage;

        private String text;
        private List<Fact> facts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Fact {
        private String name;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Action {
        @JsonProperty("@type")
        private String type;

        private String name;
        private List<Target> targets;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Target {
        private String os;
        private String uri;
    }
}
