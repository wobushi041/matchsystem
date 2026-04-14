package com.wobushi041.matchsystem.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    protected int pageNum;
    protected int pageSize;
}
