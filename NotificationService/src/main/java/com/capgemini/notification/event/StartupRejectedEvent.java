package com.capgemini.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartupRejectedEvent {
    private Long startupId;
    private Long founderId;
    private String startupName;
}
