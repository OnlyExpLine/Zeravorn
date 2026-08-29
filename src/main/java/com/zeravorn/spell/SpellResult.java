package com.zeravorn.spell;

import com.zeravorn.map.Position;

public record SpellResult(boolean accepted, String errorCode, Position destination, int amount) {
    static SpellResult rejected(String code) { return new SpellResult(false, code, null, 0); }
    static SpellResult success(Position destination, int amount) { return new SpellResult(true, "", destination, amount); }
}
