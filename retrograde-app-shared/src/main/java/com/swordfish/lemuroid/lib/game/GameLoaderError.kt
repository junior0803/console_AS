package com.swordfish.lemuroid.lib.game

class GameLoaderException(val error: GameLoaderError) : RuntimeException("Game Loader error: $error")

enum class GameLoaderError {
    GL_INCOMPATIBLE,
    GENERIC,
    MISSING_BIOS,
    LOAD_CORE,
    LOAD_GAME,
    SAVES
}
