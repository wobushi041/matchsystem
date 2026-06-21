package com.wobushi041.matchsystem.model.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 6218947034899114472L;

    /**
     * 页码
     */
    protected int pageNum = 1;



    /**
     * 一页多少行，每页记录数
     */
    protected int pageSize = 10;
}
