package org.example.demo1.entity.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiCustomEventResponse {

    private List<AiCustomEventItem> events = new ArrayList<>();

    public void setEvents(List<AiCustomEventItem> events) {
        this.events = events == null ? new ArrayList<>() : events;
    }
}
