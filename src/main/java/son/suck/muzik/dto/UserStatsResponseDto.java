package son.suck.muzik.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserStatsResponseDto {
    private long totalGames;
    private long winCount;
    private double winRate;
    private int maxScore;

    public static UserStatsResponseDto of(long totalGames, long winCount, Integer maxScore) {
        // 0판일 때 0으로 나누기 방지
        double rate = (totalGames == 0) ? 0.0 : ((double) winCount / totalGames) * 100;
        // 소수점 첫째자리까지 반올림
        double roundedRate = Math.round(rate * 10.0) / 10.0;

        return new UserStatsResponseDto(
                totalGames,
                winCount,
                roundedRate,
                maxScore != null ? maxScore : 0
        );
    }
}
