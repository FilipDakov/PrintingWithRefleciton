
package com.nvexis.namesdemo;

import java.lang.reflect.Field;

public class Person {
    private int id ;
    private String name;
    private boolean pool;

    public Person(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public void setPool(boolean pool) {
        this.pool = pool;
    }

    public boolean getPool(){
        return this.pool;
    }

}
