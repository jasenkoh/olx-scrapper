package org.olxscrapper.domain;


import lombok.Getter;

@Getter
public class IndexChunk {
    private final long start;
    private final long end;

    public IndexChunk(long start, long end) {
        this.start = start;
        this.end = end;
    }
}