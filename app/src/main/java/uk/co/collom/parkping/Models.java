package uk.co.collom.parkping;

import java.util.List;

final class Models {
    private Models() { }
    record Park(String id, String name, String timezone) {
        @Override public String toString() { return name; }
    }
    static final List<Park> PARKS = java.util.Arrays.asList(
        new Park("75ea578a-adc8-4116-a54d-dccb60765ef9", "Magic Kingdom", "America/New_York"),
        new Park("47f90d2c-e191-4239-a466-5892ef59a88b", "EPCOT", "America/New_York"),
        new Park("288747d1-8b4f-4a64-867e-ea7c9b27bad8", "Hollywood Studios", "America/New_York"),
        new Park("1c84a229-8862-4648-9c71-378ddd2c7693", "Animal Kingdom", "America/New_York"),
        new Park("dae968d5-630d-4719-8b06-3d107e944401", "Disneyland Paris", "Europe/Paris"),
        new Park("ca888437-ebb4-4d50-aed2-d227f7096968", "Disney Adventure World", "Europe/Paris"),
        new Park("eb3f4560-2383-4a36-9152-6b3e5ed6bc57", "Universal Studios Florida", "America/New_York"),
        new Park("267615cc-8943-4c2a-ae2c-5da728ca591f", "Islands of Adventure", "America/New_York"),
        new Park("12dbb85b-265f-44e6-bccf-f1faa17211fc", "Epic Universe", "America/New_York")
    );
    record Ride(String id, String name, double latitude, double longitude,
                Integer waitMinutes, String status, long updatedAt) { }
}
