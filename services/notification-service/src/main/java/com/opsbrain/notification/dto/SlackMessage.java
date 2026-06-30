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
public class SlackMessage {

    private String text;
    private List<Block> blocks;

    @JsonProperty("thread_ts")
    private String threadTs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Block {
        private String type;
        private Text text;
        private List<Field> fields;
        private Image image;

        @JsonProperty("block_id")
        private String blockId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Text {
        private String type;
        private String text;
        private Boolean emoji;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Field {
        private String type;
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Image {
        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("alt_text")
        private String altText;
    }
}
