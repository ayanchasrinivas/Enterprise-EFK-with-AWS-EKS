package com.opsbrain.postmortem.model;

public enum PostmortemFormat {
    PDF("pdf"),
    MARKDOWN("markdown"),
    HTML("html");

    private final String extension;

    PostmortemFormat(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
