function handlePlayCard(cardidx) {
    //TODO: Needs implementation
}

function handleSkipDogLife(button) {
    // TODO needs implementation
}
function startGame() {
    sendEvent("StartGame")
}

function handleTrumpSelection(object) {
    const $button = $(object);
    const trumpIndex = parseInt($button.data('trump'));
    const payload = {
        suitIndex: trumpIndex
    }
    sendEvent("PickTrumpsuit", payload)

}
function handleKickPlayer(playerId) {
    sendEvent("KickPlayer", {
        playerId: playerId
    })
}
function handleReturnToLobby() {
    sendEvent("ReturnToLobby")
}

globalThis.startGame = startGame
globalThis.handleTrumpSelection = handleTrumpSelection
globalThis.handleKickPlayer = handleKickPlayer
globalThis.handleReturnToLobby = handleReturnToLobby