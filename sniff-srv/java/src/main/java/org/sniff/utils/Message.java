package org.sniff.utils;

import lombok.Data;

import java.util.List;

@Data
public class Message<T> {

    private String               database;
    private String               table;
    private List<String>         pkNames;
    private Boolean              isDdl;
    private String               type;
    private Long                 es;
    private Long                 ts;
    private String               sql;
    private List<T> data;
    private List<T> old;
}
