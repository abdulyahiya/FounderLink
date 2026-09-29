package com.capgemini.startup.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupRejectedEvent implements Serializable {

    private Long startupId;
    private Long founderId;
    private String startupName;
}
