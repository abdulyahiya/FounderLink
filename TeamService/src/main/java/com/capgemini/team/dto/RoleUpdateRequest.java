package com.capgemini.team.dto;

import com.capgemini.team.enums.TeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpdateRequest {

    @NotNull(message = "Role is required")
    private TeamRole role;
}
