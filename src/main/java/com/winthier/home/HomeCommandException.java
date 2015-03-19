package com.winthier.home;

import lombok.Getter;
import lombok.NonNull;

class HomeCommandException extends RuntimeException {
    @Getter
    private final Message homeMessage;

    public HomeCommandException(@NonNull Message homeMessage) {
        super("Uncaught HomeCommandException with message for " + homeMessage.getRecipient());
        this.homeMessage = homeMessage;
    }
}
