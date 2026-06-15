package org.example.demo1.entity.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CustomEventRequest {

    private String eventID;
    private String yearTerm;
    private List<String> weekList = new ArrayList<>();
    private String weekDay;
    private String sessionStart;
    private String sessionLast;
    private String eventName;
    private String address;
    private String memberName;

    public void setWeekList(List<String> weekList) {
        this.weekList = weekList == null ? new ArrayList<>() : weekList;
    }
}
