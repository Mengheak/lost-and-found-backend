package com.group5.lostandfoundjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "icon_url")
    private String iconUrl;

    /** Required by JPA. */
    protected Category() {}

    public Category(String name, String iconUrl) {
        this.name = name;
        this.iconUrl = iconUrl;
    }
}
