package son.suck.muzik.service;

import son.suck.muzik.dto.MafiaNightActionRequestDto;
import son.suck.muzik.dto.MafiaVoteRequestDto;

public class MafiaPlayServiceImpl implements MafiaPlayService{
    @Override
    public void processNightAction(MafiaNightActionRequestDto request) {

    }

    @Override
    public void calculateNightResult(Long roomId) {

    }

    @Override
    public void processVote(MafiaVoteRequestDto request) {

    }

    @Override
    public void calculateDayResult(Long roomId) {

    }

    @Override
    public boolean checkGameEndCondition(Long roomId) {
        return false;
    }
}
