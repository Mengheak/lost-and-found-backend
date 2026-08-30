package com.group5.lostandfoundjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "text")
    private String text;

    @Column(name = "image_url")
    private String imageUrl;

    /** Required by JPA. */
    protected Message() {}

    public Message(Conversation conversation, User sender, String text, String imageUrl) {
        this.conversation = conversation;
        this.sender = sender;
        this.text = text;
        this.imageUrl = imageUrl;
    }
}
