package common;

import java.io.Serializable;

public enum GameState implements Serializable {
    WAITING_FOR_PLAYERS,
    STARTING,
    WORD_DISTRIBUTION,
    ROUND_1,
    ROUND_2,
    ROUND_3,
    VOTING,
    RESULT,
    GAME_OVER
}
