package ru.boomearo.headfetcher.json;

import lombok.Data;

import java.util.List;

@Data
public class SkinProfileJson {

    private String id;
    private String name;
    private List<Property> properties;
    private List<Object> profileActions;

    @Data
    public static class Property {
        private String name;
        private String value;
    }

}

