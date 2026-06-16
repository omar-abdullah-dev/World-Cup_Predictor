package com.worldcup.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Lightweight team reference returned inside fixtures, standings, etc. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamRefDto {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("logo")
    private String logo;

    public int getId()       { return id; }
    public String getName()  { return name; }
    public String getLogo()  { return logo; }

    public void setId(int id)          { this.id = id; }
    public void setName(String name)   { this.name = name; }
    public void setLogo(String logo)   { this.logo = logo; }

    @Override
    public String toString() {
        return "TeamRefDto{id=" + id + ", name='" + name + "'}";
    }
}
