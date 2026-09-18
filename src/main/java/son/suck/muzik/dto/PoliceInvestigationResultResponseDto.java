package son.suck.muzik.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PoliceInvestigationResultResponseDto {
    private Long targetId;
    private String targetNickname;
    @JsonProperty("isMafia")
    private boolean isMafia;         // true: 마피아 맞음, false: 마피아 아님
}
