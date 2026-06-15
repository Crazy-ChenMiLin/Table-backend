package org.example.demo1.entity.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchCustomEventRequest {

    private List<CustomEventRequest> events = new ArrayList<>();

    public void setEvents(List<CustomEventRequest> events) {
        this.events = events == null ? new ArrayList<>() : events;
    }
}
